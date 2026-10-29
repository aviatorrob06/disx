package xyz.ar06.disx.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSource;
import xyz.ar06.disx.audio_filters.DisxAudioFilterType;
import xyz.ar06.disx.DisxLogger;
import xyz.ar06.disx.exceptions.*;
import xyz.ar06.disx.utils.DisxInternetCheck;
import xyz.ar06.disx.DisxServerAudioRegistry;
import xyz.ar06.disx.DisxSystemMessages;
import xyz.ar06.disx.config.DisxConfigHandler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import xyz.ar06.disx.utils.DisxWWW2PCMReference;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class DisxSoundCommand {

    public static void registerCommand(){
        CommandRegistrationEvent.EVENT.register(((dispatcher, registry, selection) -> {
            LiteralCommandNode<CommandSourceStack> register = dispatcher.register(Commands.literal("disxsound")
                    .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                            .then(Commands.argument("dimension", DimensionArgument.dimension())
                                    .then(Commands.argument("position", BlockPosArgument.blockPos())
                                            .then(Commands.argument("startTime", IntegerArgumentType.integer())
                                                    .then(Commands.argument("volume", IntegerArgumentType.integer(0, 200))
                                                            .then(Commands.argument("loop", BoolArgumentType.bool())
                                                                    .then(Commands.argument("audioUrl", StringArgumentType.greedyString())
                                                                    .executes(DisxSoundCommand::run))))))));
        }));
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        if (!context.getSource().hasPermission(1)){
            context.getSource().sendFailure(Component.translatable("sysmsg.disx.cmd_no_permission"));
        } else {
            try {
                if (context.getSource().isPlayer()){
                    if (DisxConfigHandler.SERVER.isOnUseBlacklist(context.getSource().getPlayer().getUUID())){
                        throw new DisxPlayerBlacklistedException("Player blacklisted by server!");
                    }
                    if (!DisxConfigHandler.SERVER.isOnUseWhitelist(context.getSource().getPlayer().getUUID())){
                        throw new DisxPlayerNotWhitelistedException("Player not whitelisted by server!");
                    }
                }
                int currentAudioPlayerCount = DisxServerAudioRegistry.getRegistryCount();
                int maxAudioPlayerCount = Integer.valueOf(DisxConfigHandler.SERVER.getProperty("max_audio_players"));
                if (currentAudioPlayerCount >= maxAudioPlayerCount){
                    throw new DisxConcurrentAudioNodeCeilingException("Too many audio nodes presently!");
                }
                context.getSource().sendSystemMessage(Component.translatable("sysmsg.disx.soundcmd.one_moment"));
                CompletableFuture.runAsync(() -> runAsync(context));
            } catch (DisxException e){
                DisxLogger.error(e);
                DisxSystemMessages.disxError((CommandSource) context.getSource(), e);
                return 0;
            }
        }
        return 1;
    }

    private static void runAsync(CommandContext<CommandSourceStack> context){
        try {
            String videoId = context.getArgument("audioUrl", String.class);
            ResourceLocation dimension = context.getArgument("dimension", ResourceLocation.class);
            BlockPos blockPos = BlockPosArgument.getBlockPos(context, "position");
            Integer startTime = context.getArgument("startTime", Integer.class);
            Integer volumePercentage = context.getArgument("volume", Integer.class);
            Boolean loop = context.getArgument("loop", Boolean.class);
            int rogueRadius = -1;
            ArrayList<DisxAudioFilterType> audioFilters = new ArrayList<DisxAudioFilterType>();
            DisxLogger.debug("START TIME PROVIDED: " + startTime);
            boolean hasInternet = DisxInternetCheck.checkInternet();
            if (!hasInternet){
                throw new DisxNoInternetException("No Internet Detected!");
            }
            if (DisxServerAudioRegistry.isNodeAtLocation(blockPos, dimension)){
                throw new DisxConcurrentAudioNodeException("Already audio playing at this block pos!");
            }
            DisxWWW2PCMReference www2PCMReference = new DisxWWW2PCMReference(videoId);
            int videoLength = www2PCMReference.probeDuration();
            if (videoLength > 1800) {
                throw new DisxTooLongException("Duration too long!");
            }
            if (!context.getSource().isPlayer()){
                DisxServerAudioRegistry.addToRegistry(blockPos, videoId, null, context.getSource().getServer(), dimension, startTime.intValue(), loop, volumePercentage, rogueRadius, audioFilters);
            } else {
                DisxServerAudioRegistry.addToRegistry(blockPos, videoId, context.getSource().getPlayer(), context.getSource().getServer(), dimension, startTime.intValue(), loop, volumePercentage, rogueRadius, audioFilters);
            }
            context.getSource().sendSystemMessage(Component.translatable("sysmsg.disx.soundcmd.attempting_playback", videoId, blockPos.toString(), dimension.toString()));
        } catch (DisxException e){
            if (context.getSource().isPlayer()){
                DisxSystemMessages.disxError(context.getSource().getPlayer(), e);
                DisxLogger.error(e);
            } else {
                DisxSystemMessages.disxError(context.getSource().getServer(), e);
                DisxLogger.error(e);
            }
        }
    }

}
