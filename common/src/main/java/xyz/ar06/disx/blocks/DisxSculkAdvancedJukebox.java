package xyz.ar06.disx.blocks;

import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import xyz.ar06.disx.DisxLogger;
import xyz.ar06.disx.entities.DisxSculkAdvancedJukeboxEntity;
import xyz.ar06.disx.items.DisxCustomDisc;
import xyz.ar06.disx.utils.DisxJukeboxUsageCooldownManager;
import xyz.ar06.disx.utils.DisxSculkAdvancedJukeboxInventoryManager;

public class DisxSculkAdvancedJukebox extends BaseEntityBlock {
    public static RegistrySupplier<Block> blockRegistration;
    public static RegistrySupplier<Item> itemRegistration;
    public static RegistrySupplier<BlockEntity> blockEntityRegistration;

    public DisxSculkAdvancedJukebox(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (!level.isClientSide){
            DisxLogger.debug("Detected use attempt; level is not client side");
            if (interactionHand.equals(InteractionHand.MAIN_HAND) && !DisxJukeboxUsageCooldownManager.isOnCooldown(blockPos, level.dimension())){
                DisxLogger.debug("Interaction hand is main, no cooldown detected");
                ItemStack handStack = player.getItemInHand(interactionHand);
                BlockEntity blockEntity = level.getBlockEntity(blockPos);
                if (blockEntity instanceof DisxSculkAdvancedJukeboxEntity jukeboxEntity){
                    DisxLogger.debug("block entity detected is correct");
                    DisxSculkAdvancedJukeboxInventoryManager inventoryManager = DisxSculkAdvancedJukeboxInventoryManager.get(level.getServer());
                    if (!(handStack.getItem() instanceof DisxCustomDisc)){
                        DisxLogger.debug("Player is not holding a custom disc");
                        if (jukeboxEntity.getItem(0).equals(ItemStack.EMPTY) && player.isCrouching()  && !(handStack.isEmpty())){
                            DisxLogger.debug("No frequency stack in jukebox, player holding item, setting one");
                            jukeboxEntity.setItem(0, handStack.copyWithCount(1));
                            handStack.shrink(1);
                            level.playSound(null, blockPos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1F, 1F);
                            jukeboxEntity.setChanged();
                            return InteractionResult.SUCCESS;
                        } else if (player.isCrouching() && !(jukeboxEntity.getItem(0).equals(ItemStack.EMPTY))){
                            DisxLogger.debug("Frequency stack found in jukebox, removing");
                            ItemStack returnStack = jukeboxEntity.removeItem(0, 0);
                            player.addItem(returnStack);
                            level.playSound(null, blockPos, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1F, 1F);
                            jukeboxEntity.setChanged();
                            return InteractionResult.SUCCESS;
                        } else if (!jukeboxEntity.getItem(0).equals(ItemStack.EMPTY)){
                            DisxLogger.debug("Frequency stack is not empty");
                            if (!inventoryManager.getDiscInventory(jukeboxEntity.getItem(0)).equals(ItemStack.EMPTY)){
                                DisxLogger.debug("Disc inventory is not empty, returning disc");
                                ItemStack returnStack = inventoryManager.removeDiscFromInventory(jukeboxEntity.getItem(0).copy(), level);
                                player.addItem(returnStack);
                                level.playSound(null, blockPos, SoundEvents.SCULK_CATALYST_BREAK, SoundSource.BLOCKS, 1F, 1F);
                                return InteractionResult.SUCCESS;
                            }
                        }
                    } else if (handStack.getItem() instanceof DisxCustomDisc customDisc){
                        DisxLogger.debug("Hand item is custom disc");
                        if (!jukeboxEntity.getItem(0).equals(ItemStack.EMPTY)){
                            DisxLogger.debug("Frequency stack not empty");
                            if (inventoryManager.getDiscInventory(jukeboxEntity.getItem(0)).equals(ItemStack.EMPTY)){
                                DisxLogger.debug("Disc inventory empty, putting disc in");
                                inventoryManager.setDiscInventory(jukeboxEntity.getItem(0).copy(), handStack.copyWithCount(1), level);
                                handStack.shrink(1);
                                level.playSound(null, blockPos, SoundEvents.SCULK_CATALYST_PLACE, SoundSource.BLOCKS, 1F, 1F);
                                vibrationParticle(level, blockPos);
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
            return InteractionResult.FAIL;
        }
        return super.use(blockState, level, blockPos, player, interactionHand, blockHitResult);
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        if (!level.isClientSide()) {
            DisxSculkAdvancedJukeboxEntity entity = (DisxSculkAdvancedJukeboxEntity) level.getBlockEntity(blockPos);
            if (entity == null) {
                DisxLogger.debug("block destroy initialized, no block entity found");
            } else {
                if (entity.isHas_frequencyStack()) {
                    DisxLogger.debug("block destroy initialized, has frequency factor; removing it and dropping it");
                    ItemStack newItemStack = entity.removeItem(0, 1);
                    ItemEntity itemEntity = new ItemEntity(entity.getLevel(), blockPos.getX(), blockPos.getY(), blockPos.getZ(), newItemStack);
                    itemEntity.setDefaultPickUpDelay();
                    level.addFreshEntity(itemEntity);
                }
            }
        }
        super.onRemove(blockState, level, blockPos, blockState2, bl);
    }

    private void vibrationParticle(Level level, BlockPos blockPos){
        if (level instanceof ServerLevel serverLevel){
            serverLevel.playSound(null, blockPos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 1F, 1F);
            serverLevel.sendParticles(new VibrationParticleOption(new BlockPositionSource(blockPos.north(3).west(3).below(3)), 32), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            serverLevel.sendParticles(new VibrationParticleOption(new BlockPositionSource(blockPos.east(3).north(3).below(3)), 32), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            serverLevel.sendParticles(new VibrationParticleOption(new BlockPositionSource(blockPos.south(3).east(3).below(3)), 32), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            serverLevel.sendParticles(new VibrationParticleOption(new BlockPositionSource(blockPos.west(3).south(3).below(3)), 32), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            serverLevel.sendParticles(new VibrationParticleOption(new BlockPositionSource(blockPos.north(3).west(3).above(3)), 32), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            serverLevel.sendParticles(new VibrationParticleOption(new BlockPositionSource(blockPos.east(3).north(3).above(3)), 32), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            serverLevel.sendParticles(new VibrationParticleOption(new BlockPositionSource(blockPos.south(3).east(3).above(3)), 32), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            serverLevel.sendParticles(new VibrationParticleOption(new BlockPositionSource(blockPos.west(3).south(3).above(3)), 32), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);

        }
    }

    public static void registerBlock(Registrar<Block> registrar){
        blockRegistration = registrar.register(
                new ResourceLocation("disx","sculk_advanced_jukebox"),
                () -> new DisxSculkAdvancedJukebox(BlockBehaviour.Properties.of().instabreak().sound(SoundType.SCULK_SENSOR).strength(1F).explosionResistance(0F))
        );
    }

    public static void registerBlockItem(Registrar<Item> itemRegistrar){
        itemRegistration = itemRegistrar.register(
                new ResourceLocation("disx","sculk_advanced_jukebox"),
                () -> new BlockItem(blockRegistration.get(), new Item.Properties())
        );
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DisxSculkAdvancedJukeboxEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return super.getTicker(level, blockState, blockEntityType);
    }

    @Override
    public @Nullable <T extends BlockEntity> GameEventListener getListener(ServerLevel serverLevel, T blockEntity) {
        return super.getListener(serverLevel, blockEntity);
    }
}
