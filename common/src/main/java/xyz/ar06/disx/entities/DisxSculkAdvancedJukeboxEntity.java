package xyz.ar06.disx.entities;

import dev.architectury.registry.registries.Registrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;
import xyz.ar06.disx.DisxMain;
import xyz.ar06.disx.blocks.DisxSculkAdvancedJukebox;

public class DisxSculkAdvancedJukeboxEntity extends BlockEntity implements ContainerSingleItem, WorldlyContainer {

    NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    public DisxSculkAdvancedJukeboxEntity(BlockPos blockPos, BlockState blockState) {
        super(DisxMain.REGISTRAR_MANAGER.get().get(Registries.BLOCK_ENTITY_TYPE).get(
                new ResourceLocation("disx","sculk_advanced_jukebox")),
                blockPos,
                blockState
        );
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return false;
    }

    @Override
    public ItemStack getItem(int i) {
        return inventory.get(0);
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        ItemStack returnStack = inventory.get(0).copy();
        inventory.set(0, ItemStack.EMPTY);
        return returnStack;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        inventory.set(0, itemStack);
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void load(CompoundTag compoundTag) {
        ContainerHelper.loadAllItems(compoundTag, inventory);
        super.load(compoundTag);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        ContainerHelper.saveAllItems(compoundTag, inventory);
        super.saveAdditional(compoundTag);
    }

    public static void registerEntity(Registrar<BlockEntityType<?>> registrar){
        registrar.register(
                new ResourceLocation("disx","sculk_advanced_jukebox"),
                () -> BlockEntityType.Builder.of(DisxSculkAdvancedJukeboxEntity::new, DisxSculkAdvancedJukebox.blockRegistration.get()).build(null)
        );
    }

    public boolean isHas_frequencyFactor(){
        return !(this.inventory.get(0).isEmpty());
    }

}
