package xyz.ar06.disx.commands;

import xyz.ar06.disx.DisxLogger;
import xyz.ar06.disx.DisxSystemMessages;
import xyz.ar06.disx.exceptions.*;
import xyz.ar06.disx.items.DisxRecordStamp;
import xyz.ar06.disx.utils.DisxWWW2PCMReference;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class DisxStampCommand {

    public static void registerCommand(){
        CommandRegistrationEvent.EVENT.register(((dispatcher, registry, selection) -> {
            dispatcher.register(Commands.literal("disxstamp")
                            .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                    .then(Commands.argument("player", EntityArgument.players())
                            .then(Commands.argument("videoUrl", StringArgumentType.greedyString())
                                    .executes(DisxStampCommand::run))));
        }));
    }
    private static int run(CommandContext<CommandSourceStack> context){
        if (!context.getSource().hasPermission(1)){
            context.getSource().sendFailure(Component.translatable("sysmsg.disx.cmd_no_permission"));
            return 1;
        } else {
            context.getSource().sendSystemMessage(Component.translatable("sysmsg.disx.stampcmd.one_moment"));
            CompletableFuture.runAsync(() -> runAsync(context));
        }
        return 1;
    }

    private static void runAsync(CommandContext<CommandSourceStack> context){
        String videoId = context.getArgument("videoUrl", String.class);
        Collection<ServerPlayer> playerCollection;
        try {
            playerCollection = EntityArgument.getPlayers(context, "player");
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            context.getSource().sendFailure(Component.translatable("sysmsg.disx.stampcmd.no_player_err"));
            throw new RuntimeException(e);
        }
        try {
            DisxWWW2PCMReference www2PCMReference = new DisxWWW2PCMReference(videoId);
            String videoName = www2PCMReference.probeTitle();
            int videoLength = www2PCMReference.probeDuration();
            if (videoLength > 1800) {
                throw new DisxTooLongException("Duration: " + videoLength);
            }
            Item item = DisxRecordStamp.getItemRegistration().get();
            ItemStack recordStampStack = new ItemStack(item);
            recordStampStack.setCount(1);
            CompoundTag tag = new CompoundTag();
            tag.putString("videoId", videoId);
            tag.putString("videoName", videoName);
            recordStampStack.setTag(tag);
            for (ServerPlayer plr : playerCollection){
                plr.playNotifySound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.MASTER, 1, 1);
                plr.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1f, 1f);
                plr.addItem(recordStampStack);
            }
            context.getSource().sendSystemMessage(Component.translatable("sysmsg.disx.stampcmd.success"));
        } catch (DisxNoInternetException e){
            if (context.getSource().isPlayer()){
                DisxSystemMessages.noMediaFound(context.getSource().getPlayer());
            } else {
                DisxSystemMessages.noMediaFound(context.getSource().getServer());
            }
            DisxLogger.error(e);
        } catch (DisxWWW2PCMException e){
            if (e.wasResourceNotFound()){
                if (context.getSource().isPlayer()){
                    DisxSystemMessages.noMediaFound(context.getSource().getPlayer());
                } else {
                    DisxSystemMessages.noMediaFound(context.getSource().getServer());
                }
            } else {
                if (context.getSource().isPlayer()){
                    DisxSystemMessages.apiError(context.getSource().getPlayer());
                } else {
                    DisxSystemMessages.apiError(context.getSource().getServer());
                }
            }
            DisxLogger.error(e);
        } catch (DisxInvalidDiscTypeException e){
            if (context.getSource().isPlayer()){
                DisxSystemMessages.invalidDiscType(context.getSource().getPlayer());
            } else {
                DisxSystemMessages.invalidDiscType(context.getSource().getServer());
            }
            DisxLogger.error(e);
        } catch (DisxTooLongException e){
            if (context.getSource().isPlayer()){
                DisxSystemMessages.badDuration(context.getSource().getPlayer());
            } else {
                DisxSystemMessages.badDuration(context.getSource().getServer());
            }
            DisxLogger.error(e);
        } catch (DisxMalformedURLException e){
            if (context.getSource().isPlayer()){
                DisxSystemMessages.badURL(context.getSource().getPlayer());
            } else {
                DisxSystemMessages.badURL(context.getSource().getServer());
            }
            DisxLogger.error(e);
        } catch (Exception e){
            DisxLogger.error(e);
        }

    }
}
