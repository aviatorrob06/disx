package xyz.ar06.disx.utils;

import org.json.JSONObject;
import xyz.ar06.disx.DisxLogger;
import xyz.ar06.disx.DisxModInfo;
import xyz.ar06.disx.DisxTmpHandler;

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
import java.time.Duration;
import java.util.Locale;

public class DisxWWW2PCMReference {

    private String mediaUrl;
    private String apiAccessUrl;
    private DisxWWW2PCMSourceType service;
    private String identifier;

    public DisxWWW2PCMReference(String url){
        this.mediaUrl = url;
        this.apiAccessUrl = DisxModInfo.getWww2pcmURL();

        parseUrl();
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
            }

        } catch (IllegalArgumentException | URISyntaxException e) {
            // Malformed URL; leave the reference INVALID.
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

    public File resolveAudioFile(){
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
        String reqUrl = this.apiAccessUrl + this.service.getValue() + "/pcm?" + apiArg + "=" + this.identifier;
        try {
            DisxLogger.debug("Audio file resolve requested. Checking tmp directory caches");
            String expectedFilePath = "./.disx-tmp/cache/" + this.service.getValue() + "_" + this.identifier + ".pcm";
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
                DisxLogger.debug("Got response from WWW2PCM-API. Downloading audio...");
                InputStream in = response.body();
                File newFile = new File(expectedFilePath);
                Files.copy(in, Path.of(expectedFilePath), StandardCopyOption.REPLACE_EXISTING);
                return newFile;
            } else {
                DisxLogger.error("Disx Error: WWW2PCM-API response failed. Status Code: " + response.statusCode());
                DisxLogger.error(response.body());
                return null;
            }

        } catch (Exception e){
            e.printStackTrace();
            return null;
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
        if (this.service.equals(DisxWWW2PCMSourceType.INVALID)){
            throw new RuntimeException("INVALID URL");
        }
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
                return jsonObject;
            } else {
                DisxLogger.error("Disx Error: WWW2PCM-API metadata response failed. Status Code: " + response.statusCode());
                DisxLogger.error(response.body());
                throw new RuntimeException(response.body());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String probeTitle(){
        try {
            JSONObject metadata = resolveMetadata();
            String title = metadata.getString("title");
            return title;
        } catch (RuntimeException e){
            return "Video Not Found";
        }
    }

    public int probeDuration(){
        try {
            JSONObject metadata = resolveMetadata();
            int duration = metadata.getInt("duration");
            return duration;
        } catch (RuntimeException e){
            return -1;
        }
    }

}
