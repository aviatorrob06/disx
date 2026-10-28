package xyz.ar06.disx.utils;

import org.json.JSONObject;
import xyz.ar06.disx.DisxLogger;
import xyz.ar06.disx.DisxModInfo;
import xyz.ar06.disx.DisxTmpHandler;
import xyz.ar06.disx.exceptions.DisxMalformedURLException;
import xyz.ar06.disx.exceptions.DisxWWW2PCMException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DisxWWW2PCMReference {

    private String mediaUrl;
    private String apiAccessUrl;
    private DisxWWW2PCMSourceType service;
    private String identifier;
    private String hashedFileName;
    private JSONObject cachedMetadata;

    private static final AtomicInteger threadCounter = new AtomicInteger(1);
    private final ExecutorService www2PCMExecutor =
            Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "WWW2PCM/Thread-" + threadCounter.getAndIncrement()
                );

                thread.setDaemon(true);
                return thread;
            });

    public DisxWWW2PCMReference(String url) throws DisxWWW2PCMException, DisxMalformedURLException {
        this.mediaUrl = url;
        this.apiAccessUrl = DisxModInfo.getWww2pcmURL();

        parseUrl();

        this.hashedFileName = hashUrl(this.mediaUrl);
    }

    private void parseUrl(){
        service = DisxWWW2PCMSourceType.INVALID;
        identifier = "";

        if (mediaUrl == null || mediaUrl.isBlank()) {
            return;
        }

        try {
            URI uri = URI.create(mediaUrl.trim()).parseServerAuthority();

            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();

            if (scheme == null || host == null
                    || !(scheme.equalsIgnoreCase("http")
                    || scheme.equalsIgnoreCase("https"))
                    || uri.getUserInfo() != null) {
                return;
            }

            host = host.toLowerCase(Locale.ROOT);

            if (path == null) {
                path = "";
            }

            String[] segments = path.split("/");

            // YouTube
            if (host.equals("youtu.be")) {
                String id = firstSegment(segments, 1);

                if (!id.isBlank()) {
                    setReference(DisxWWW2PCMSourceType.YOUTUBE, id);
                }
                return;
            }

            if (isHost(host, "youtube.com")) {
                String id = null;

                if (path.equals("/watch")) {
                    id = getQueryParameter(uri, "v");
                } else if (segments.length >= 3
                        && (segments[1].equals("shorts")
                        || segments[1].equals("embed")
                        || segments[1].equals("live"))) {
                    id = segments[2];
                }

                if (id != null && !id.isBlank()
                        && !id.contains("/")) {
                    setReference(DisxWWW2PCMSourceType.YOUTUBE, id);
                }
                return;
            }

            // Spotify: track URLs only
            if (isHost(host, "spotify.com")) {
                for (int i = 1; i < segments.length - 1; i++) {
                    if (segments[i].equals("track")) {
                        String id = segments[i + 1];

                        if (!id.isBlank()) {
                            setReference(DisxWWW2PCMSourceType.SPOTIFY, id);
                        }
                        return;
                    }
                }

                return;
            }

            // SoundCloud: preserve the resource path
            if (isHost(host, "soundcloud.com")) {
                String resourcePath = path.replaceAll("/+$", "");

                if (!resourcePath.isBlank()
                        && !resourcePath.equals("/")) {
                    setReference(DisxWWW2PCMSourceType.SOUNDCLOUD,
                            resourcePath.substring(1));
                }
                return;
            }

            // Bandcamp: artist/track_id
            if (host.endsWith(".bandcamp.com")
                    && !host.equals("bandcamp.com")) {

                String artist = host.substring(
                        0, host.length() - ".bandcamp.com".length());

                for (int i = 1; i < segments.length - 1; i++) {
                    if (segments[i].equals("track")) {
                        String trackId = segments[i + 1];

                        if (!artist.contains(".")
                                && !artist.isBlank()
                                && !trackId.isBlank()) {
                            setReference(DisxWWW2PCMSourceType.BANDCAMP,
                                    artist + "/" + trackId);
                        }
                        return;
                    }
                }

                return;
            }

            // Direct URLs: only if enabled in your mod's configuration.
            // Replace this condition with your actual config setting.
            if (DisxModInfo.isDirectUrlEnabled()) {
                setReference(DisxWWW2PCMSourceType.DIRECT, uri.toASCIIString());
            } else {
                throw new DisxMalformedURLException("Direct URL not enabled on this server!");
            }

        } catch (IllegalArgumentException | URISyntaxException e) {
            throw new DisxMalformedURLException(e.getMessage());
        }
    }

    private void setReference(DisxWWW2PCMSourceType source, String id) {
        this.service = source;
        this.identifier = id;
    }

    private static boolean isHost(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private static String firstSegment(String[] segments, int index) {
        return index < segments.length ? segments[index] : "";
    }

    private static String getQueryParameter(URI uri, String name) {
        String query = uri.getRawQuery();

        if (query == null) {
            return null;
        }

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8);

            if (key.equals(name)) {
                return parts.length > 1
                        ? URLDecoder.decode(
                        parts[1], StandardCharsets.UTF_8)
                        : "";
            }
        }

        return null;
    }

    public DisxWWW2PCMSourceType getService() {
        return service;
    }

    public String getIdentifier() {
        return identifier;
    }

    public File resolveAudioFile() {
        String apiArg = switch (this.service){
            case YOUTUBE -> "video_id";
            case SPOTIFY -> "track_id";
            case SOUNDCLOUD -> "resource_path";
            case BANDCAMP -> "resource_path";
            case DIRECT -> "url";
            case INVALID -> null;
        };
        if (this.service.equals(DisxWWW2PCMSourceType.INVALID)){
            return null;
        }
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String reqUrl = this.apiAccessUrl + this.service.getValue() + "/pcm?" + apiArg + "=" + this.identifier;
                    DisxLogger.debug("Audio file resolve requested. Checking tmp directory caches");
                    String expectedFilePath = "./.disx-tmp/cache/" + this.hashedFileName +".pcm";
                    File returnFile = new File(expectedFilePath);
                    if (returnFile.exists()){
                        Thread.sleep(1000); //sleep for a second to ensure most clients got their audio lines ready for data streaming
                        //only because the sleep needs to happen when the audio is loaded from cached
                        DisxLogger.debug("Found cached audio file. Returning");
                        DisxTmpHandler.markAudioUsed(returnFile);
                        return returnFile;
                    }
                    DisxLogger.debug("Did not find in cache. Requesting from WWW2PCM-API");
                    HttpClient httpClient = HttpClient.newBuilder()
                            .followRedirects(HttpClient.Redirect.ALWAYS)
                            .connectTimeout(Duration.ofSeconds(20))
                            .build();
                    HttpRequest request = HttpRequest.newBuilder(URI.create(reqUrl))
                            .GET()
                            .build();
                    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() == 200){
                        DisxLogger.debug("Got valid response from WWW2PCM-API");

                        JSONObject jsonObject = new JSONObject(response.body());
                        if (!jsonObject.isEmpty()){
                            throw new DisxWWW2PCMException(jsonObject.getJSONObject("detail").getString("error"), response.statusCode(), true);
                        }
                        DisxLogger.debug("No errors found. Downloading audio...");
                        InputStream in = response.body();
                        File newFile = new File(expectedFilePath);

                        Files.copy(in, Path.of(expectedFilePath), StandardCopyOption.REPLACE_EXISTING);

                        return newFile;
                    } else {
                        if (response.statusCode() == 403){
                            JSONObject jsonObject = new JSONObject(response.body());
                            if (jsonObject.has("detail")){
                                throw new DisxWWW2PCMException(jsonObject.getJSONObject("detail").getString("error"), response.statusCode(), true);
                            } else {
                                throw new DisxWWW2PCMException("Error in API response", response.statusCode());
                            }
                        } else {
                            throw new DisxWWW2PCMException("Error in API response", response.statusCode());
                        }
                    }
                } catch (IOException | InterruptedException e){
                    throw new DisxWWW2PCMException(Arrays.toString(e.getStackTrace()));
                }

            }, www2PCMExecutor).get();
        } catch (Exception e){
            throw new DisxWWW2PCMException(Arrays.toString(e.getStackTrace()));
        }
    }

    private JSONObject resolveMetadata() throws RuntimeException{
        String apiArg = switch (this.service){
            case YOUTUBE -> "video_id";
            case SPOTIFY -> "track_id";
            case SOUNDCLOUD -> "resource_path";
            case BANDCAMP -> "resource_path";
            case DIRECT -> "url";
            case INVALID -> null;
        };
        if (cachedMetadata != null){
            return cachedMetadata;
        }
        if (this.service.equals(DisxWWW2PCMSourceType.INVALID)){
            throw new DisxMalformedURLException("Malformed URL!");
        }
        try {
            return CompletableFuture.supplyAsync(() -> {
                String reqUrl = this.apiAccessUrl + this.service.getValue() + "/metadata?" + apiArg + "=" + this.identifier;
                DisxLogger.debug("REQ URL: " + reqUrl);
                try {
                    HttpClient httpClient = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder(URI.create(reqUrl))
                            .GET()
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200){
                        JSONObject jsonObject = new JSONObject(response.body());
                        if (jsonObject.has("detail")){
                            if (jsonObject.getJSONObject("detail").has("error")){
                                throw new DisxWWW2PCMException(jsonObject.getJSONObject("detail").getString("error"), response.statusCode(), true);
                            }
                        }
                        cachedMetadata = jsonObject;
                        return jsonObject;
                    } else {
                        if (response.statusCode() == 403){
                            JSONObject jsonObject = new JSONObject(response.body());
                            if (jsonObject.has("detail")){
                                throw new DisxWWW2PCMException(jsonObject.getJSONObject("detail").getString("error"), response.statusCode(), true);
                            } else {
                                throw new DisxWWW2PCMException("Error in API response", response.statusCode());
                            }
                        } else {
                            throw new DisxWWW2PCMException("Error in API response", response.statusCode());
                        }
                    }

                } catch (IOException | InterruptedException e) {
                    throw new DisxWWW2PCMException(Arrays.toString(e.getStackTrace()));
                }
            }, www2PCMExecutor).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DisxWWW2PCMException(Arrays.toString(e.getStackTrace()));
        }
    }

    public String probeTitle() throws DisxWWW2PCMException{
        JSONObject metadata = resolveMetadata();
        if (metadata.has("title")){
            String title = metadata.getString("title");
            return title;
        } else {
            return "null";
        }
    }

    public int probeDuration() throws DisxWWW2PCMException{
        JSONObject metadata = resolveMetadata();
        Number duration = metadata.getNumber("duration");
        return duration.intValue();
    }

    public String getHashedFileName() {
        return hashedFileName;
    }

    private static String hashUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or blank");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by Java, so this should never happen.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

}
