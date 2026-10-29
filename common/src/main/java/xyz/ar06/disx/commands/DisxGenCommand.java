package xyz.ar06.disx.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import xyz.ar06.disx.DisxLogger;
import xyz.ar06.disx.exceptions.*;
import xyz.ar06.disx.utils.*;
import xyz.ar06.disx.DisxMain;
import xyz.ar06.disx.DisxSystemMessages;
import xyz.ar06.disx.commands.suggestionProviders.DisxTypeSuggestionProvider;
import xyz.ar06.disx.items.DisxCustomDisc;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class DisxGenCommand {
    //OLD TITLE SCRAPER
    //static YoutubeDownloader ytDownloader = new YoutubeDownloader();

    public static void registerCommand(){
        CommandRegistrationEvent.EVENT.register(((dispatcher, registry, selection) -> {
            LiteralCommandNode<CommandSourceStack> register = dispatcher.register(Commands.literal("disxgen")
                    .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                            .then(Commands.argument("player", EntityArgument.players())
                                    .then(Commands.argument("discType", StringArgumentType.string()).suggests(DisxTypeSuggestionProvider::getSuggestions)
                                            .then(Commands.argument("audioUrl", StringArgumentType.greedyString())
                                                    .executes(DisxGenCommand::run)))));
        ;}));
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        Logger logger = LoggerFactory.getLogger("disx");
        DisxLogger.debug("Command Run");
        if (!context.getSource().hasPermission(1)){
            context.getSource().sendFailure(Component.translatable("sysmsg.disx.cmd_no_permission"));
        } else
        {
            context.getSource().sendSystemMessage(Component.translatable("sysmsg.disx.gencmd.one_moment"));
            CompletableFuture.runAsync(() -> runAsync(context));
        }
        return 1;
    }

    private static void runAsync(CommandContext<CommandSourceStack> context){
        String argumentResult = context.getArgument("discType", String.class);
        String videoId = context.getArgument("audioUrl", String.class);
        try {
            boolean hasInternet = DisxInternetCheck.checkInternet();
            if (!hasInternet){
                throw new DisxNoInternetException("No internet detected!");
            }
            boolean validDisc = false;
            for (String element : DisxCustomDisc.validTypes) {
                if (element.equals(argumentResult)){
                    validDisc = true;
                }
            }
            if (!validDisc){
                throw new DisxInvalidDiscTypeException("Given Type: " + argumentResult);
            }
            Item disc = DisxMain.REGISTRAR_MANAGER.get().get(Registries.ITEM).get(new ResourceLocation("disx", "custom_disc_" + argumentResult));
            ItemStack stack = disc.getDefaultInstance();
            stack.setCount(1);
            CompoundTag stackNbt = stack.getOrCreateTag();
            videoId.replace(" ", "");
            stackNbt.putString("videoId", videoId);
            DisxLogger.debug(videoId);
            //OLD TITLE SCRAPER
            //RequestVideoInfo videoInfoRequest = new RequestVideoInfo(videoId);
            //DisxLogger.debug("videoInfoRequest Generated");
            //Response<VideoInfo> videoInfoResponse = null;
            //DisxLogger.debug("videoInfoResponse initialized");
            //videoInfoResponse = ytDownloader.getVideoInfo(videoInfoRequest);
            DisxWWW2PCMReference www2PCMReference = new DisxWWW2PCMReference(videoId);
            String mediaTitle = www2PCMReference.probeTitle();
            int mediaLength = www2PCMReference.probeDuration();
            DisxLogger.debug("video length is " + mediaLength);
            if (mediaLength > 1800) {
                throw new DisxTooLongException("Duration: " + mediaLength);
            }
            stackNbt.putString("discName", mediaTitle);
            stack.setTag(stackNbt);
            Collection<ServerPlayer> playerCollection = EntityArgument.getPlayers(context, "player");
            for (ServerPlayer player : playerCollection){
                player.playNotifySound(SoundEvents.PISTON_EXTEND, SoundSource.MASTER, 1, 1);
                player.getInventory().add(stack);
            }
            context.getSource().sendSystemMessage(Component.translatable("sysmsg.disx.gencmd.success"));
            DisxLogger.debug("success??..." + "custom_disc_" + argumentResult);
        } catch (DisxException e){
            if (context.getSource().isPlayer()){
                DisxSystemMessages.disxError(context.getSource().getPlayer(), e);
            } else {
                DisxSystemMessages.disxError(context.getSource().getServer(), e);
            }
            DisxLogger.error(e);
        } catch (CommandSyntaxException e){
            DisxLogger.error(e);
            context.getSource().sendFailure(Component.literal(e.getMessage()));
        }
    }


}
