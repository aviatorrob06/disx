package xyz.ar06.disx;


import com.google.common.io.Files;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xyz.ar06.disx.audio_filters.*;
import xyz.ar06.disx.exceptions.DisxAudioStreamingNodeException;
import xyz.ar06.disx.exceptions.DisxMalformedURLException;
import xyz.ar06.disx.exceptions.DisxWWW2PCMException;
import xyz.ar06.disx.utils.DisxWWW2PCMReference;

import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static xyz.ar06.disx.DisxAudioFormatConstants.*;

public class DisxAudioStreamingNode {
    private static final AtomicInteger threadCounter = new AtomicInteger(1);
    private static final ExecutorService audioSendExecutor =
            Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "Disx Audio-Send Thread-" + threadCounter.getAndIncrement()
                );

                thread.setDaemon(true);
                return thread;
            });

    //@Deprecated private AudioPlayer audioPlayer = new DefaultAudioPlayer(playerManager);
    //private AudioInputStream inputStream = AudioPlayerInputStream.createStream(audioPlayer, LPformat, 20000, true);;
    private AudioInputStream inputStream;// = AudioPlayerInputStream.createStream(audioPlayer, LPformat, 20000, true);;
    private File audioFile;

    private BlockPos blockPos;
    private MinecraftServer minecraftServer;
    private ResourceLocation dimension;
    private Player nodeOwner;
    private boolean loop;
    private String mediaUrl;
    private DisxWWW2PCMReference www2PCMReference;

    private int preferredVolume;
    private boolean paused = false;
    @Deprecated boolean useLiveYtSrc = false;
    private int rogueRadius;

    private DisxAudioMotionType motionType;
    private UUID entityUuid;

    private ArrayList<DisxAudioFilterType> activeFilters;


    public DisxAudioStreamingNode(String mediaUrl, BlockPos blockPos, MinecraftServer minecraftServer, ResourceLocation dimension, Player nodeOwner, boolean loop, int startTime, DisxAudioMotionType motionType, UUID entityUuid, int rogueRadius, ArrayList<DisxAudioFilterType> audioFilters){
        DisxLogger.debug("New Audio Streaming Node called for; setting details (MOTION TYPE: " + motionType.name() + "); Running Async!");
        this.mediaUrl = mediaUrl;
        this.blockPos = blockPos;
        this.minecraftServer = minecraftServer;
        this.dimension = dimension;
        this.nodeOwner = nodeOwner;
        this.loop = loop;
        //this.audioPlayer.addListener(new TrackHandler());
        this.preferredVolume = 100;
        this.motionType = motionType;
        this.entityUuid = entityUuid;
        this.rogueRadius = rogueRadius;
        this.activeFilters = audioFilters;
        try {
            this.www2PCMReference = new DisxWWW2PCMReference(this.mediaUrl);
        } catch (DisxMalformedURLException e){
            DisxLogger.error(e);
            return;
        }
        //this.useLiveYtSrc = true; //DisxModInfo.isUseYtsrc();
        CompletableFuture.runAsync(() -> {
            //DisxLogger.debug(this.useLiveYtSrc ? "Server is configured to use live YouTube source; setting load URL" : "Server is configured to use Disx-YTSRC-API; setting load URL");
            //String url = this.useLiveYtSrc ? "https://www.youtube.com/watch?v=" + videoId : "http://disxytsourceapi.ar06.xyz/stream_audio?id=" + videoId;

            try {
                File audioFile = null;
                if (!this.activeFilters.isEmpty()){
                    DisxLogger.debug("Detected audio filters active; checking for already-processed audio");
                    List<File> filesStream = java.nio.file.Files.list(Path.of(DisxTmpHandler.TMP_PROCESSED_CACHE_PATH))
                            .map(path -> new File(path.toUri()))
                            .toList();
                    for (File file : filesStream){
                        String fileName = file.getName();
                        HashMap<DisxAudioFilterType, Boolean> matchedEffects = new HashMap<>();
                        for (DisxAudioFilterType queriedFilter : this.activeFilters){
                            if (fileName.contains("." + queriedFilter.name()) && fileName.contains(this.www2PCMReference.getHashedFileName())){
                                matchedEffects.put(queriedFilter, true);
                            } else {
                                matchedEffects.put(queriedFilter, false);
                            }
                        }
                        if (!matchedEffects.containsValue(false)){
                            DisxLogger.debug("Found cached processed file, using it");
                            audioFile = file;
                            DisxTmpHandler.markAudioUsed(file);
                            Thread.sleep(1000);
                            break;
                        }
                    }
                    if (audioFile == null){
                        DisxLogger.debug("Did not find cached processed file, requesting regular version");
                        File downloadFile = this.www2PCMReference.resolveAudioFile();
                        byte[] receivedData = Files.toByteArray(downloadFile);
                        DisxLogger.debug("Beginning audio processing");
                        StringBuilder cachePathBuilder = new StringBuilder(DisxTmpHandler.TMP_PROCESSED_CACHE_PATH + "/"
                                + this.www2PCMReference.getHashedFileName());
                        for (DisxAudioFilterType filter : this.getActiveFilters()){
                            cachePathBuilder.append(".").append(filter.name());
                            DisxLogger.debug("Detected " + filter.name() + " filter, applying it to audio");
                            receivedData = filter.create().process(receivedData);
                        }
                        cachePathBuilder.append(".pcm");
                        File cacheFile = new File(cachePathBuilder.toString());
                        Files.write(receivedData, cacheFile);
                        DisxLogger.debug("Processing done; storing audio cache and passing to audio streamer");
                        audioFile = cacheFile;
                    }
                } else {
                    DisxLogger.debug("Requesting audio file from resolver");
                    audioFile = this.www2PCMReference.resolveAudioFile();
                }
                if (audioFile == null || !audioFile.exists()) throw new IOException("File does not exist");
                DisxLogger.debug("Starting data stream loop");
                this.audioFile = audioFile;
                DisxAudioStreamingNode.this.streamAudioData();
            } catch (DisxWWW2PCMException e){
                if (e.wasResourceNotFound()){
                    DisxSystemMessages.noMediaFound(nodeOwner);
                } else {
                    DisxSystemMessages.apiError(nodeOwner);
                }
                DisxLogger.error(e);
                DisxServerAudioRegistry.removeFromRegistry(DisxAudioStreamingNode.this);
            } catch (IOException | InterruptedException e){
                DisxLogger.error(e);
            }


        }, audioSendExecutor);

    }

    private void streamAudioData() {
            try {
                byte[] buffer = new byte[chunkSize];
                DisxServerPacketIndex.ServerPackets.playingAudioMessage(DisxAudioStreamingNode.this.www2PCMReference.getIdentifier(), DisxAudioStreamingNode.this.nodeOwner);
                if (inputStream == null){
                    DisxLogger.debug("Input stream found null; making new one");
                    this.inputStream = new AudioInputStream(new FileInputStream(audioFile), format, (audioFile.length() / format.getFrameSize()));
                }
                int bytesRead;
                while (inputStream != null && !this.isPaused()){
                    bytesRead = inputStream.read(buffer);
                    if (bytesRead != 0 && bytesRead != -1){
                        for (Player p : DisxServerAudioRegistry.getMcPlayers()) {
                            if (this.motionType.equals(DisxAudioMotionType.STATIC)){
                                Vec3 plrPos = p.getEyePosition();
                                Vec3 blockPosVec = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                                double distance = plrPos.distanceTo(blockPosVec);
                                double maxPacketDistance = (double) (DisxModInfo.getAudioRadius() + 10);
                                if (distance <= maxPacketDistance){
                                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                                    buf.writeBlockPos(this.blockPos);
                                    buf.writeResourceLocation(this.dimension);
                                    buf.writeUtf(this.motionType.name());
                                    buf.writeUUID(this.entityUuid);
                                    buf.writeBytes(buffer, 0, bytesRead);
                                    DisxServerPacketIndex.ServerPackets.audioData(p, buf);
                                }
                            } else {
                                Vec3 plrPos = p.getEyePosition();
                                Vec3 entityPos = null;
                                for (ServerLevel level : minecraftServer.getAllLevels()){
                                    Entity entity = level.getEntity(this.entityUuid);
                                    if (entity != null){
                                        entityPos = entity.getPosition(1F);
                                    }
                                }
                                if (entityPos != null){
                                    double distance = plrPos.distanceTo(entityPos);
                                    double maxPacketDistance = (double) (DisxModInfo.getAudioRadius() + 10);
                                    if (distance <= maxPacketDistance) {
                                        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                                        buf.writeBlockPos(this.blockPos);
                                        buf.writeResourceLocation(this.dimension);
                                        buf.writeUtf(this.motionType.name());
                                        buf.writeUUID(this.entityUuid);
                                        buf.writeBytes(buffer, 0, bytesRead);
                                        DisxServerPacketIndex.ServerPackets.audioData(p, buf);
                                    }
                                } else {
                                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                                    buf.writeBlockPos(this.blockPos);
                                    buf.writeResourceLocation(this.dimension);
                                    buf.writeUtf(this.motionType.name());
                                    buf.writeUUID(this.entityUuid);
                                    buf.writeBytes(buffer, 0, bytesRead);
                                    DisxServerPacketIndex.ServerPackets.audioData(p, buf);
                                }

                            }

                        }
                        Thread.sleep((long) (streamInterval * 1000L));
                    } else {
                        break;
                    }
                }
                if (this.isPaused()){
                    DisxLogger.debug("Audio was paused; streaming should stop");
                    //lastPosition = currentPosition;
                } else if (this.motionType == null){
                    DisxLogger.debug("Audio streaming stopped; audio node deregistered?");
                    //DisxServerAudioRegistry.removeFromRegistry(DisxAudioStreamingNode.this);
                } else {
                    try {
                        if (loop){
                            DisxLogger.debug("Audio streaming finished; loop == true; calling for track replay and restream");
                            //this.audioPlayer.playTrack(cachedTrack.makeClone());
                            this.streamAudioData();
                        } else {
                            DisxLogger.debug("Audio streaming finished; loop != true; waiting to deregister audio node (many seconds) if not already done so");
                            Thread.sleep(((long) streamInterval * 1000L * 2L));
                            DisxLogger.debug("deregistering audio node if not already done so");
                            DisxServerAudioRegistry.removeFromRegistry(DisxAudioStreamingNode.this);
                        }
                    } catch (Exception e) {
                        DisxAudioStreamingNodeException exception = new DisxAudioStreamingNodeException(e.getMessage());
                        DisxLogger.error(exception);
                    }
                }

            } catch (Exception e) {
                DisxLogger.error(e);
            }
    }

    public void setDimension(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void deconstruct(){
        /*if (this.audioPlayer != null){
            this.audioPlayer.stopTrack();
            this.audioPlayer.destroy();
            this.audioPlayer = null;
        }*/
        this.blockPos = null;
        this.dimension = null;
        this.entityUuid = null;
        this.motionType = null;
        this.loop = false;
        //this.nodeOwner = null;
        this.mediaUrl = null;
        this.paused = false;
        try {
            if (this.inputStream != null){
                this.inputStream.close();
                this.inputStream = null;
            }
            if (this.audioFile != null){
                //this.audioDataCache.close();
                this.audioFile = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pausePlayer(){
        this.paused = true;
    }

    public void resumePlayer(){
        this.paused = false;
        this.streamAudioData();
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public Player getNodeOwner() {
        return nodeOwner;
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean isPaused(){
        return this.paused;
    }


    public int incrementVolume(double amount){
        int currentVolume = this.preferredVolume;
        int castedAmount = (int) amount;
        int newVolume = currentVolume + (castedAmount * 10);
        if (newVolume > 200){
            newVolume = 200;
        }
        if (newVolume < 0){
            newVolume = 0;
        }
        return this.preferredVolume = newVolume;
    }

    public int getPreferredVolume() {
        return preferredVolume;
    }

    public static double getStreamInterval() {
        return streamInterval;
    }

    public DisxAudioMotionType getMotionType() {
        return motionType;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public int getRogueRadius() { return rogueRadius; }

    public ArrayList<DisxAudioFilterType> getActiveFilters() { return activeFilters; }


}
