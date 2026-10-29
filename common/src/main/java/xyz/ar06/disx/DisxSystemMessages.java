package xyz.ar06.disx;

import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import xyz.ar06.disx.exceptions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DisxSystemMessages {
    public static void onPlayReadyNoInternet(Player player) {
        player.sendSystemMessage(Component.translatable("sysmsg.disx.no_internet").withStyle(ChatFormatting.RED));
    }

    public static void noInternetErrorMessage(Player player){
        ServerPlayer serverPlayer = (ServerPlayer) player;
        serverPlayer.sendSystemMessage(Component.translatable("sysmsg.disx.no_internet").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
    }

    public static void noInternetErrorMessage(MinecraftServer server){
        server.sendSystemMessage(Component.literal("Disx Error: No internet connection!").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
    }

    public static void errorLoading(Player player){
        ServerPlayer serverPlayer = (ServerPlayer) player;
        serverPlayer.sendSystemMessage(Component.translatable("sysmsg.disx.error_loading_vid").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
    }

    public static void errorLoading(LocalPlayer player){
        player.sendSystemMessage(Component.translatable("sysmsg.disx.error_loading_vid").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
    }

    public static void disxError(CommandSource recipient, DisxException exception){
        if (recipient instanceof Player | recipient instanceof MinecraftServer){
            if (exception instanceof DisxWWW2PCMException e){
                if (e.wasTooManyRequests()){
                    recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_api_ratelimit").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
                } else if (e.wasBadRequest()){
                    recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_api_bad_request").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
                } else if (e.wasMediaNotFound()){
                    recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_api_no_match").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
                } else if (e.wasForbiddenRequest()){
                    recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_api_request_forbidden").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
                } else if (e.wasInternalServerError()){
                    recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_api_internal_error").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
                } else if (e.wasUnreachableServer()) {
                    recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_api_no_response").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
                } else if (e.serverTookTooLong()){
                    recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_api_took_too_long").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
                } else {
                    recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_api_general").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
                }
            } else if (exception instanceof DisxNoInternetException e){
                recipient.sendSystemMessage(Component.translatable("sysmsg.disx.no_internet").withStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
            } else if (exception instanceof DisxInvalidDiscTypeException e){
                recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_invalid_disc_type").withStyle(ChatFormatting.RED));
            } else if (exception instanceof DisxTooLongException e){
                recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_bad_duration", "30").withStyle(ChatFormatting.RED));
            } else if (exception instanceof DisxMalformedURLException e){
                recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_bad_url", "30").withStyle(ChatFormatting.RED));
            } else if (exception instanceof DisxConcurrentAudioNodeCeilingException e){
                recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_max_audio_count").withStyle(ChatFormatting.RED));
            } else if (exception instanceof DisxDimensionBlacklistedException e){
                recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_dimension_blacklisted").withStyle(ChatFormatting.RED));
            } else if (exception instanceof DisxPlayerBlacklistedException e){
                recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_blacklisted").withStyle(ChatFormatting.RED));
            } else if (exception instanceof DisxPlayerNotWhitelistedException e){
                recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_not_whitelisted").withStyle(ChatFormatting.RED));
            } else if (exception instanceof DisxConcurrentAudioNodeException e){
                recipient.sendSystemMessage(Component.translatable("sysmsg.disx.error_concurrent_audio").withStyle(ChatFormatting.RED));
            }
        }
    }

    public static void disxErrorAsync(MinecraftServer server, BlockPos blockPos, String playerName, DisxException e){
        disxError(server, e);
        server.sendSystemMessage(Component.literal("FUNCTION WAS ASYNCHRONOUS, BLOCKPOS: " + blockPos + ", PLAYER NAME (IF INVOLVED): " + playerName));
    }

    public static void playingAtLocation(MinecraftServer server, String playerName, BlockPos pos, String mediaUrl, ResourceLocation dimension){
        server.sendSystemMessage(Component.literal("" + playerName + " is playing '" + mediaUrl +"' at " + pos.toString() + " in " + dimension.toString()));
    }

    public static void loadingMedia(String audioId){
        if (audioId.length() > 19){
            audioId = audioId.substring(0, 20) + "...";
        }
        sendOverlayMessage(Component.translatable("sysmsg.disx.loading_media", audioId).withStyle(ChatFormatting.ITALIC), false);
    }

    public static void playingMedia(String audioId){
        if (audioId.length() > 19){
            audioId = audioId.substring(0, 20) + "...";
        }
        Minecraft.getInstance().gui.setNowPlaying(Component.translatable("sysmsg.disx.playing_media", audioId));
    }

    public static void sendOverlayMessage(MutableComponent mutableComponent, boolean animated){
        Minecraft.getInstance().gui.setOverlayMessage(mutableComponent, animated);
    }

    public static void loopStatusMsg(boolean enabled){
        MutableComponent enabledMessage = Component.translatable("sysmsg.disx.loop_enabled").withStyle(ChatFormatting.GRAY);
        MutableComponent disabledMessage = Component.translatable("sysmsg.disx.loop_disabled").withStyle(ChatFormatting.GRAY);
        MutableComponent toSendMessage = enabled ? enabledMessage : disabledMessage;
        sendOverlayMessage(toSendMessage, false);
    }

    public static void pauseStatusMsg(boolean paused){
        MutableComponent enabledMessage = Component.translatable("sysmsg.disx.paused_audio").withStyle(ChatFormatting.GRAY);
        MutableComponent disabledMessage = Component.translatable("sysmsg.disx.resumed_audio").withStyle(ChatFormatting.GRAY);
        MutableComponent toSendMessage = paused ? enabledMessage : disabledMessage;
        sendOverlayMessage(toSendMessage, false);
    }

    public static void mutedAlready(){
        Minecraft.getInstance().player.sendSystemMessage(Component.translatable("sysmsg.disx.mutecmd.error").withStyle(ChatFormatting.RED));
    }

    public static void notMuted(){
        Minecraft.getInstance().player.sendSystemMessage(Component.translatable("sysmsg.disx.unmutecmd.error").withStyle(ChatFormatting.RED));
    }

    public static void successfulMutation(){
        Minecraft.getInstance().player.sendSystemMessage(Component.translatable("sysmsg.disx.mutecmd.success"));
    }

    public static void successfulUnmutation(){
        Minecraft.getInstance().player.sendSystemMessage(Component.translatable("sysmsg.disx.unmutecmd.success"));
    }

    public static void debugStatus(boolean b){
        if (b){
            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("sysmsg.disx.debug_enabled"));
            Minecraft.getInstance().player.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER);
        } else {
            Minecraft.getInstance().player.sendSystemMessage(Component.translatable("sysmsg.disx.debug_disabled"));
            Minecraft.getInstance().player.playSound(SoundEvents.TOTEM_USE);
        }
    }

    static List<TextColor> volMsgColors = List.of(
            TextColor.fromRgb(0x00FF00),
            TextColor.fromRgb(0x00E600),
            TextColor.fromRgb(0x00CD00),
            TextColor.fromRgb(0x00B400),
            TextColor.fromRgb(0x009B00),
            TextColor.fromRgb(0x008200),
            TextColor.fromRgb(0x006900),
            TextColor.fromRgb(0x005000),
            TextColor.fromRgb(0x003700),
            TextColor.fromRgb(0x000500)
    );
    static TextColor volMsgEmptyColor = TextColor.fromRgb(0x000000);

    public static void volumeSetMessage(int volume){
        if (volume > 200){
            volume = 200;
        }
        int length = volume / 10;
        if (length > 10){
            length = 10;
        }
        MutableComponent message = Component.literal("VOL: [").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
        int index = length - 1;
        for (int i = 0; i < 10; i++){
            if (index >= 0){
                message.append(Component.literal("▮").withStyle(Style.EMPTY.withColor(volMsgColors.get(index))));
                index--;
            } else {
                message.append(Component.literal("▮").withStyle(Style.EMPTY.withColor(volMsgEmptyColor)));
            }
        }
        message.append(Component.literal("] (").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        message.append(Component.literal(String.valueOf(volume)));
        message.append(Component.literal("%)").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        sendOverlayMessage(message, false);
    }


    public static void outdatedModVersion(MinecraftServer server){
        if (!DisxModInfo.getIsUpToDate()){
            ArrayList<MutableComponent> messages = new ArrayList<MutableComponent>();
            messages.add(Component.translatable("sysmsg.disx.notice_outdated").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            messages.add(Component.translatable("sysmsg.disx.outdated_version_info", DisxModInfo.getVERSION(), DisxModInfo.getLatestVersion()).withStyle(ChatFormatting.GRAY));
            messages.add(Component.translatable("sysmsg.disx.outdated_version_count", DisxModInfo.getVersionsOutdated()).withStyle(ChatFormatting.ITALIC));
            messages.add(Component.translatable("sysmsg.disx.outdated_consider_updating"));
            messages.add(Component.translatable("sysmsg.disx.outdated_run_disxinfo"));
            if (server.isSingleplayer()){
                PlayerEvent.PLAYER_JOIN.register(player -> {
                    for (MutableComponent component : messages){
                        player.sendSystemMessage(component);
                    }
                    player.sendSystemMessage(Component.empty());
                });
            } else {
                for (MutableComponent component : messages){
                    server.sendSystemMessage(component);
                }
                PlayerEvent.PLAYER_JOIN.register(player -> {
                    if (player.hasPermissions(1)){
                        for (MutableComponent component : messages){
                            player.sendSystemMessage(component);
                        }
                        player.sendSystemMessage(Component.empty());
                    }
                });
            }
        }
    }

    public static void potentialModConflict(MinecraftServer server) {
        String instructionsURL_CARRYON = DisxModInfo.getCarryonConfigInstructionsUrl();

        HashMap<String, Boolean> conflicts = DisxModInfo.getPotentialModConflicts();
        boolean oneConflictFound = false;
        ArrayList<MutableComponent> messages = new ArrayList<MutableComponent>();
        for (Object o : conflicts.keySet()) {
            if (conflicts.get(o).equals(true)) {
                if (oneConflictFound == false) {
                    oneConflictFound = true;
                    messages.add(Component.translatable("sysmsg.disx.notice_potential_conflict").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                }
                if (o.toString().equals("carryon")) {
                    MutableComponent instructionsLink = Component.literal("here").withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.DARK_AQUA).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, instructionsURL_CARRYON)));
                    if (server.isDedicatedServer()){
                        instructionsLink = instructionsLink.append(Component.literal(" (" + instructionsURL_CARRYON + ")"));
                    }
                    messages.add(Component.translatable("sysmsg.disx.conflict_carryon", instructionsLink));
                }
            }
        }
        if (server.isSingleplayer()){
            PlayerEvent.PLAYER_JOIN.register(player -> {
                for (MutableComponent component : messages){
                    player.sendSystemMessage(component);
                }
                if (!messages.isEmpty()){
                    player.sendSystemMessage(Component.empty());
                }

            });
        } else {
            for (MutableComponent component : messages){
                server.sendSystemMessage(component);
            }
            PlayerEvent.PLAYER_JOIN.register(player -> {
                if (player.hasPermissions(1)){
                    for (MutableComponent component : messages){
                        player.sendSystemMessage(component);
                    }
                    if (!messages.isEmpty()){
                        player.sendSystemMessage(Component.empty());
                    }
                }
            });
        }
    }

    public static void devBuildNotice(MinecraftServer server){
        if (DisxModInfo.getIsDevBuild()){
            MutableComponent message = Component.translatable("sysmsg.disx.notice_devbuild")
                    .withStyle(ChatFormatting.GOLD)
                    .withStyle(ChatFormatting.BOLD);
            MutableComponent message2 = Component.translatable("sysmsg.disx.devbuild_installed_version", DisxModInfo.getVERSION())
                    .withStyle(ChatFormatting.GRAY);
            if (server.isSingleplayer()){
                PlayerEvent.PLAYER_JOIN.register(player -> {
                    player.sendSystemMessage(message);
                    player.sendSystemMessage(message2);
                    player.sendSystemMessage(Component.empty());
                });
            } else {
                server.sendSystemMessage(message);
                server.sendSystemMessage(message2);
                PlayerEvent.PLAYER_JOIN.register(player -> {
                    if (player.hasPermissions(1)){
                        player.sendSystemMessage(message);
                        player.sendSystemMessage(message2);
                        player.sendSystemMessage(Component.empty());
                    }
                });
            }
        }

    }



}
