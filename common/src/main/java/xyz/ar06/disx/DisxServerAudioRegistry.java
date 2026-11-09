package xyz.ar06.disx;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import xyz.ar06.disx.audio_filters.DisxAudioFilterType;
import xyz.ar06.disx.config.DisxConfigHandler;
import xyz.ar06.disx.entities.DisxSculkAdvancedJukeboxEntity;
import xyz.ar06.disx.exceptions.*;
import xyz.ar06.disx.utils.DisxInternetCheck;

import java.util.*;

public class DisxServerAudioRegistry {

    public static class FrequencyRegistry{
        public static HashMap<ItemStack, ArrayList<DisxSculkAdvancedJukeboxEntity>> entitiesOnFrequency = new HashMap<>();

        public static void addEntityToFrequency(ItemStack frequencyStack, DisxSculkAdvancedJukeboxEntity entity){
            DisxLogger.debug("Adding DisxSculkAdvancedJukeboxEntity to frequency registry");
            ItemStack keyFrequencyStack = findFrequencyKey(frequencyStack);
            if (keyFrequencyStack == null){
                ArrayList<DisxSculkAdvancedJukeboxEntity> toAdd = new ArrayList<>();
                toAdd.add(entity);
                entitiesOnFrequency.put(frequencyStack, toAdd);
                for (Player player : players){
                    DisxServerPacketIndex.ServerPackets.frequencySync(frequencyStack, entity.getBlockPos(), entity.getLevel().dimension().location(), true, player);
                }
            } else {
                ArrayList<DisxSculkAdvancedJukeboxEntity> toAdd = entitiesOnFrequency.get(keyFrequencyStack);
                if (toAdd.contains(entity)){
                    return;
                }
                toAdd.add(entity);
                entitiesOnFrequency.replace(frequencyStack, toAdd);
                for (Player player : players){
                    DisxServerPacketIndex.ServerPackets.frequencySync(frequencyStack, entity.getBlockPos(), entity.getLevel().dimension().location(), true, player);
                }
            }
        }

        public static void removeEntityFromFrequency(ItemStack frequencyStack, DisxSculkAdvancedJukeboxEntity entity){
            DisxLogger.debug("Removing DisxSculkAdvancedJukeboxEntity from frequency registry");
            ItemStack keyFrequencyStack = findFrequencyKey(frequencyStack);
            if (keyFrequencyStack != null){
                entitiesOnFrequency.get(keyFrequencyStack).remove(entity);
                for (Player player : players){
                    DisxServerPacketIndex.ServerPackets.frequencySync(frequencyStack, entity.getBlockPos(), entity.getLevel().dimension().location(), false, player);
                }
            }
        }

        public static ArrayList<DisxSculkAdvancedJukeboxEntity> getEntitiesOnFrequency(ItemStack frequencyStack){
            ItemStack keyFrequencyStack = findFrequencyKey(frequencyStack);
            if (keyFrequencyStack != null){
                return entitiesOnFrequency.get(keyFrequencyStack);
            } else {
                return new ArrayList<>();
            }
        }

        private static ItemStack findFrequencyKey(ItemStack frequencyStack) {
            for (ItemStack key : entitiesOnFrequency.keySet()) {
                if (ItemStack.isSameItemSameTags(key, frequencyStack)) {
                    return key;
                }
            }

            return null;
        }

    }

    public static LinkedList<Player> players = new LinkedList<>();
    public static LinkedList<DisxAudioStreamingNode> registry = new LinkedList<>();

    //Default variant
    public static void addToRegistry(BlockPos pos, String mediaUrl, Player player, MinecraftServer minecraftServer, ResourceKey<Level> dimension, boolean loop, DisxAudioMotionType motionType, UUID entityUuid, int rogueRadius, ArrayList<DisxAudioFilterType> audioFilters, ItemStack frequencyStack){
        ResourceLocation dimensionLocation = dimension.location();
        if (player != null){
            DisxSystemMessages.playingAtLocation(minecraftServer, player.getName().getString(), pos, mediaUrl, dimensionLocation);
        }
        registry.add(new DisxAudioStreamingNode(mediaUrl, pos, minecraftServer, dimensionLocation, player, loop, motionType, entityUuid, rogueRadius, audioFilters, frequencyStack));
        if (player == null){
            players.forEach(plr -> {
                DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.add(plr, pos, dimensionLocation, new UUID(0L, 0L), loop, 100, motionType, entityUuid, rogueRadius, frequencyStack);
            });
        } else {
            players.forEach(plr -> {
                DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.add(plr, pos, dimensionLocation, player.getUUID(), loop, 100, motionType, entityUuid, rogueRadius, frequencyStack);
            });
        }

    }

    //Variant for Sound Command - Takes server, dimension as resourcelocation
    public static void addToRegistry(BlockPos pos, String mediaUrl, Player player, MinecraftServer server, ResourceLocation dimension, boolean loop, int volume, int rogueRadius, ArrayList<DisxAudioFilterType> audioFilters){
        ResourceLocation dimensionLocation = dimension;
        if (player != null){
            DisxSystemMessages.playingAtLocation(server, player.getName().getString(), pos, mediaUrl, dimensionLocation);
            registry.add(new DisxAudioStreamingNode(mediaUrl, pos, server, dimensionLocation, player, loop, DisxAudioMotionType.STATIC, new UUID(0L, 0L), rogueRadius, audioFilters, ItemStack.EMPTY));
        } else {
            DisxSystemMessages.playingAtLocation(server, "Server", pos, mediaUrl, dimensionLocation);
            registry.add(new DisxAudioStreamingNode(mediaUrl, pos, server, dimensionLocation, null, loop, DisxAudioMotionType.STATIC, new UUID(0L, 0L), rogueRadius, audioFilters, ItemStack.EMPTY));
        }

        if (player == null){
            players.forEach(plr -> {
                DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.add(plr, pos, dimensionLocation, new UUID(0L, 0L), loop, volume, DisxAudioMotionType.STATIC, new UUID(0L, 0L), -1, ItemStack.EMPTY);
            });
        } else {
            players.forEach(plr -> {
                DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.add(plr, pos, dimensionLocation, player.getUUID(), loop, volume, DisxAudioMotionType.STATIC, new UUID(0L, 0L), -1, ItemStack.EMPTY);
            });
        }

    }

    public static void removeFromRegistry(DisxAudioStreamingNode node){
        BlockPos pos = node.getBlockPos();
        ResourceLocation dimensionLocation = node.getDimension();
        DisxAudioMotionType motionType = node.getMotionType();
        UUID entityUuid = node.getEntityUuid();
        ItemStack frequencyStack = node.getFrequencyStack();
        registry.remove(node);
        if ((pos == null && dimensionLocation == null && motionType.equals(DisxAudioMotionType.STATIC)) ||
                (entityUuid.equals(new UUID(0L, 0L)) && motionType.equals(DisxAudioMotionType.LIVE)) ||
                (frequencyStack.isEmpty() && motionType.equals(DisxAudioMotionType.INSYNC))){
            return;
        }
        players.forEach(plr -> {
            DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.remove(plr, pos, dimensionLocation, entityUuid, motionType, frequencyStack);
        });
        node.deconstruct();
    }

    public static void removeFromRegistry(BlockPos pos, ResourceKey<Level> dimension, UUID entityUuid, DisxAudioMotionType motionType, ItemStack frequencyStack){
        DisxLogger.debug("Calling remove from registry");
        ResourceLocation dimensionLocation = dimension.location();
        players.forEach(plr -> {
            DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.remove(plr, pos, dimensionLocation, entityUuid, motionType, frequencyStack);
        });
        if (motionType.equals(DisxAudioMotionType.LIVE)){
            for (DisxAudioStreamingNode node : registry){
                if (node.getEntityUuid().equals((entityUuid)) && node.getMotionType().equals(DisxAudioMotionType.LIVE)){
                    node.deconstruct();
                    registry.remove(node);
                    break;
                }
            }
        } else if (motionType.equals(DisxAudioMotionType.STATIC)){
            for (DisxAudioStreamingNode node : registry){
                if (node.getBlockPos().equals(pos) && node.getDimension().equals(dimensionLocation) && node.getMotionType().equals(DisxAudioMotionType.STATIC)){
                    node.deconstruct();
                    registry.remove(node);
                    break;
                }
            }
        } else {
            for (DisxAudioStreamingNode node : registry){
                if (ItemStack.isSameItemSameTags(frequencyStack, node.getFrequencyStack()) && node.getMotionType().equals(DisxAudioMotionType.INSYNC)){
                    node.deconstruct();
                    registry.remove(node);
                }
            }
        }
    }

    public static void modifyEntryLoop(BlockPos blockPos, ResourceKey<Level> dimension, boolean loop){
        ResourceLocation dimensionLocation = dimension.location();
        players.forEach(plr -> {
            DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.modifyLoop(plr, blockPos, dimensionLocation, loop, DisxAudioMotionType.STATIC, new UUID(0L, 0L));
        });
        for (DisxAudioStreamingNode node : registry){
            if (node.getBlockPos().equals(blockPos) && node.getDimension().equals(dimensionLocation) && node.getMotionType().equals(DisxAudioMotionType.STATIC)){
                node.setLoop(loop);
                break;
            }
        }
    }

    public static void modifyEntryLoop(UUID entityUuid, boolean loop){
        for (DisxAudioStreamingNode node : registry){
            if (node.getEntityUuid().equals(entityUuid) && node.getMotionType().equals(DisxAudioMotionType.LIVE)){
                if (node.isLoop() != loop){
                    DisxLogger.debug("Setting loop in LIVE audio node: " + loop);
                    node.setLoop(loop);
                    players.forEach(plr -> {
                        DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.modifyLoop(plr, BlockPos.ZERO, new ResourceLocation("",""), loop, DisxAudioMotionType.LIVE, entityUuid);
                    });
                    break;
                }
            }
        }

    }

    public static boolean isNodeAtLocation(BlockPos blockPos, ResourceKey<Level> dimension){
        for (DisxAudioStreamingNode node : registry){
            if (node.getBlockPos().equals(blockPos) && node.getDimension().equals(dimension.location()) && node.getMotionType().equals(DisxAudioMotionType.STATIC)){
                return true;
            }
        }
        return false;
    }

    public static boolean isNodeAtLocation(BlockPos blockPos, ResourceLocation dimension){
        for (DisxAudioStreamingNode node : registry){
            if (node.getBlockPos().equals(blockPos) && node.getDimension().equals(dimension) && node.getMotionType().equals(DisxAudioMotionType.STATIC)){
                return true;
            }
        }
        return false;
    }

    public static boolean isNodeOnEntity(UUID entityUuid){
        for (DisxAudioStreamingNode node : registry){
            if (node.getEntityUuid().equals(entityUuid) && node.getMotionType().equals(DisxAudioMotionType.LIVE)){
                return true;
            }
        }
        return false;
    }

    public static boolean isNodeOnFrequency(ItemStack frequencyStack){
        for (DisxAudioStreamingNode node : registry){
            if (ItemStack.isSameItemSameTags(node.getFrequencyStack(), frequencyStack) && node.getMotionType().equals(DisxAudioMotionType.INSYNC)){
                return true;
            }
        }
        return false;
    }

    public static boolean isUnpausedAtLocation(BlockPos blockPos, ResourceKey<Level> dimension){
        for (DisxAudioStreamingNode node : registry){
            if (node.getBlockPos().equals(blockPos) && node.getDimension().equals(dimension.location()) && !node.isPaused() && node.getMotionType().equals(DisxAudioMotionType.STATIC)){
                return true;
            }
        }
        return false;
    }

    public static boolean isUnpausedOnEntity(UUID entityUuid){
        for (DisxAudioStreamingNode node : registry){
            if (node.getEntityUuid().equals(entityUuid) && node.getMotionType().equals(DisxAudioMotionType.LIVE) && !node.isPaused()){
                return true;
            }
        }
        return false;
    }

    public static boolean isUnpausedOnFrequency(ItemStack frequencyStack){
        for (DisxAudioStreamingNode node : registry){
            if (ItemStack.isSameItemSameTags(node.getFrequencyStack(), frequencyStack) && node.getMotionType().equals(DisxAudioMotionType.INSYNC) && !node.isPaused()){
                return true;
            }
        }
        return false;
    }


    public static void onServerClose(){
        for (DisxAudioStreamingNode node : registry){
            node.deconstruct();
        }
        registry.clear();
        FrequencyRegistry.entitiesOnFrequency.clear();
    }

    public static int getRegistryCount(){
        return registry.size();
    }

    public static void forceStopAll(){
        for (DisxAudioStreamingNode node : registry){
            removeFromRegistry(node);
        }
    }

    public static List<Player> getMcPlayers(){
        return players;
    }

    public static void pauseNode(BlockPos blockPos, ResourceKey<Level> dimension){
        for (DisxAudioStreamingNode node : registry) {
            if (node.getBlockPos().equals(blockPos) && node.getDimension().equals(dimension.location()) && node.getMotionType().equals(DisxAudioMotionType.STATIC)){
                node.pausePlayer();
                break;
            }
        }
    }

    public static void resumeNode(BlockPos blockPos, ResourceKey<Level> dimension){
        for (DisxAudioStreamingNode node : registry) {
            if (node.getBlockPos().equals(blockPos) && node.getDimension().equals(dimension.location()) && node.getMotionType().equals(DisxAudioMotionType.STATIC)){
                node.resumePlayer();
                break;
            }
        }
    }
    public static boolean pauseOrPlayNode(BlockPos blockPos, ResourceKey<Level> dimension){
        if (isUnpausedAtLocation(blockPos, dimension)){
            pauseNode(blockPos, dimension);
            return true;
        } else {
            resumeNode(blockPos, dimension);
            return false;
        }
    }

    public static void pauseNode(UUID entityUuid){
        for (DisxAudioStreamingNode node : registry) {
            if (node.getEntityUuid().equals(entityUuid) && node.getMotionType().equals(DisxAudioMotionType.LIVE)){
                node.pausePlayer();
                break;
            }
        }
    }

    public static void resumeNode(UUID entityUuid){
        for (DisxAudioStreamingNode node : registry) {
            if (node.getEntityUuid().equals(entityUuid) && node.getMotionType().equals(DisxAudioMotionType.LIVE)){
                node.resumePlayer();
                break;
            }
        }
    }
    public static boolean pauseOrPlayNode(UUID entityUuid){
        if (isUnpausedOnEntity(entityUuid)){
            pauseNode(entityUuid);
            return true;
        } else {
            resumeNode(entityUuid);
            return false;
        }
    }

    public static void incrementVolume(BlockPos blockPos, ResourceKey<Level> dimension, double amount){
        ResourceLocation dimensionLocation = dimension.location();
        for (DisxAudioStreamingNode node : registry){
            if (node.getBlockPos().equals(blockPos) && node.getDimension().equals(dimensionLocation) && node.getMotionType().equals(DisxAudioMotionType.STATIC)){
                int modifiedVol = node.incrementVolume(amount);
                for (Player player : players){
                    DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.modifyPrefVolume(player, blockPos, dimensionLocation, modifiedVol, DisxAudioMotionType.STATIC, new UUID(0L, 0L));
                }
            }
        }
    }

    public static void incrementVolume(UUID entityUuid, double amount){
        for (DisxAudioStreamingNode node : registry){
            if (node.getEntityUuid().equals(entityUuid) && node.getMotionType().equals(DisxAudioMotionType.LIVE)){
                int modifiedVol = node.incrementVolume(amount);
                for (Player player : players){
                    DisxServerPacketIndex.ServerPackets.AudioRegistrySyncPackets.modifyPrefVolume(player, BlockPos.ZERO, new ResourceLocation("", ""), modifiedVol, DisxAudioMotionType.LIVE, entityUuid);
                }
            }
        }
    }

    public static void checkAudioPrerequisiteChecks(Player player, Level level){
        if (!DisxInternetCheck.checkInternet()){
            throw new DisxNoInternetException("No internet!");
        }
        if (DisxConfigHandler.SERVER.isOnUseBlacklist(player.getUUID())){
            throw new DisxPlayerBlacklistedException("Player blacklisted!");
        }
        if (!DisxConfigHandler.SERVER.isOnUseWhitelist(player.getUUID())){
            throw new DisxPlayerNotWhitelistedException("Player not on whitelist; use-whitelist is enabled!");
        }
        if (DisxConfigHandler.SERVER.isOnDimensionBlacklist(level.dimension())){
            throw new DisxDimensionBlacklistedException("Dimension is blacklisted from audio-playing!");
        }
        int maxAudioPlayerCt = Integer.valueOf(DisxConfigHandler.SERVER.getProperty("max_audio_players"));
        int currentAudioPlayerCt = DisxServerAudioRegistry.getRegistryCount();
        if (currentAudioPlayerCt >= maxAudioPlayerCt){
            throw new DisxConcurrentAudioNodeCeilingException("Max audio player count reached!");
        }
    }

}
