package xyz.ar06.disx.entities;

import net.minecraft.world.level.block.Blocks;
import xyz.ar06.disx.DisxLogger;
import xyz.ar06.disx.exceptions.DisxMalformedURLException;
import xyz.ar06.disx.exceptions.DisxWWW2PCMException;
import xyz.ar06.disx.utils.*;
import xyz.ar06.disx.DisxMain;
import xyz.ar06.disx.DisxSystemMessages;
import xyz.ar06.disx.blocks.DisxLacquerBlock;
import xyz.ar06.disx.items.DisxRecordStamp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;


public class DisxStampMakerEntity extends BlockEntity implements Container, WorldlyContainer {

    private NonNullList<ItemStack> items;

    private String mediaUrl;

    private boolean processing = false;


    public DisxStampMakerEntity(BlockPos blockPos, BlockState blockState) {
        super(
                DisxMain.REGISTRAR_MANAGER.get().get(Registries.BLOCK_ENTITY_TYPE).get(new ResourceLocation("disx","stamp_maker_entity")),
                blockPos,
                blockState);
        this.items = NonNullList.withSize(2, ItemStack.EMPTY);
        this.processing = false;
        DisxLogger.debug("MAKING NEW ENTITY AT " + blockPos);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        boolean returnValue = true;
        for (ItemStack stack : this.items){
            if (!stack.equals(ItemStack.EMPTY)){
                returnValue = false;
            }
        }
        return returnValue;
    }

    @Override
    public ItemStack getItem(int i) {
        return this.items.get(i);
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        ItemStack stack = this.items.get(i).copy();
        this.items.set(i, ItemStack.EMPTY);
        this.processing = false;
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ItemStack returnStack = this.items.get(i).copy();
        this.items.set(i, ItemStack.EMPTY);
        return returnStack;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        if (i == 1){
            String mediaUrl = itemStack.getHoverName().getString();
            this.mediaUrl = mediaUrl;
            DisxLogger.debug("set media url to " + mediaUrl);
            DisxLogger.debug("to confirm: " + this.mediaUrl);
        } else {
            this.items.set(i, itemStack);
        }
        this.setChanged();
        checkTryAsyncProductionCond();
    }

    @Override
    public void load(CompoundTag compoundTag) {
        this.items = NonNullList.withSize(2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compoundTag, this.items);
        this.processing = compoundTag.getBoolean("processingUrl");
        super.load(compoundTag);
    }

    public void setItem(int i, ItemStack itemStack, Player player) {;
        this.items.set(i, itemStack);
        if (itemStack.getItem().equals(DisxLacquerBlock.itemRegistration.get())){
            DisxLogger.debug("lacquer block has been detected");
            if (itemStack.hasCustomHoverName()){
                DisxLogger.debug("Found lacquer block has custom name, setting media url");
                String mediaUrl = itemStack.getHoverName().getString();
                DisxLogger.debug("Detected media url: " + mediaUrl);
                this.mediaUrl = mediaUrl;
            }
            if (!this.isMediaUrlNull()){
                produceStamp(player);
            }
        }
        this.setChanged();
    }

    public void setItems(NonNullList<ItemStack> items) {
        this.items = items;
        this.setChanged();
        checkTryAsyncProductionCond();
    }


    public void checkTryAsyncProductionCond(){
        DisxLogger.debug("checking async production conditions");
        if (this.items.get(0).getItem().equals(DisxLacquerBlock.itemRegistration.get())){
            DisxLogger.debug("lacquer block found");
            if (this.items.get(0).hasCustomHoverName()){
                DisxLogger.debug("Found lacquer block has custom name, setting as media URL");
                String mediaUrl = items.get(0).getHoverName().getString();
                DisxLogger.debug("Detected media URL: " + mediaUrl);
                this.mediaUrl = mediaUrl;
            }
            if (!this.isMediaUrlNull()){
                DisxLogger.debug("media url is not null, running produce async");
                produceStampAsync();
            }
        }
    }

    @Override
    public void setChanged() {
        this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 1);
        DisxLogger.debug("should've sent block update");
        super.setChanged();
    }



    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        ContainerHelper.saveAllItems(compoundTag, this.items);
        compoundTag.putBoolean("processingUrl", this.processing);
        super.saveAdditional(compoundTag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithFullMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void setMediaUrl(String mediaUrl, Player player) {
        if (mediaUrl.equals("") || mediaUrl.equals(" ")){
            this.mediaUrl = null;
            this.setChanged();
        } else {
            this.mediaUrl = mediaUrl;
            this.setChanged();
            if (this.getItem(0).is(DisxLacquerBlock.itemRegistration.get())){
                produceStamp(player);
            }
        }
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public Boolean isMediaUrlNull(){
        if (mediaUrl == null){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    public void produceStamp(Player player){
        CompletableFuture.runAsync(() -> {
            if (!DisxInternetCheck.checkInternet()){
                DisxSystemMessages.noInternetErrorMessage(player);
            } else {
                this.processing = true;
                try {
                    this.setChanged();
                    DisxWWW2PCMReference www2PCMReference = new DisxWWW2PCMReference(this.mediaUrl);
                    String title = www2PCMReference.probeTitle();
                    int duration = www2PCMReference.probeDuration();
                    String identifier = www2PCMReference.getIdentifier();
                    if (duration > 1800) {
                        DisxSystemMessages.badDuration(player);
                        this.processing = false;
                        this.setMediaUrl("", player);
                        return;
                    }
                    if (this.items.get(0).equals(ItemStack.EMPTY)){
                        DisxLogger.debug("no lacquer block detected");
                        this.processing = false;
                        this.setMediaUrl("", player);
                        return;
                    }
                    this.setItem(0, ItemStack.EMPTY);
                    BlockPos blockPos = this.getBlockPos();
                    Level lvl = getLevel();
                    lvl.playSound(null, blockPos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS);
                    DisxRecordStamp stamp = (DisxRecordStamp) DisxMain.REGISTRAR_MANAGER.get().get(Registries.ITEM).get(new ResourceLocation("disx", "record_stamp"));
                    CompoundTag tag = new CompoundTag();
                    tag.putString("mediaUrl", this.mediaUrl);
                    tag.putString("mediaName", title);
                    ItemStack stampStack = new ItemStack(stamp);
                    stampStack.setTag(tag);
                    stampStack.setCount(1);
                    if (lvl.getBlockState(blockPos.below()).is(Blocks.HOPPER)){
                        this.items.set(1, stampStack);
                        this.mediaUrl = null;
                        lvl.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS);
                    } else {
                        ItemEntity itemEntity = new ItemEntity(getLevel(), ((double) blockPos.getX()) + 0.5, ((double) blockPos.getY()) + 0.2, ((double) blockPos.getZ()) + 0.5, stampStack);
                        itemEntity.setDefaultPickUpDelay();
                        this.mediaUrl = null;
                        lvl.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS);
                        level.addFreshEntity(itemEntity);
                    }
                } catch (DisxMalformedURLException e){
                    DisxLogger.error(e);
                    if (this.items.get(0).is(DisxLacquerBlock.itemRegistration.get())) {
                        DisxSystemMessages.badURL(player);
                    }
                } catch (DisxWWW2PCMException e){
                    DisxLogger.error(e);
                    if (e.wasResourceNotFound()){
                        if (this.items.get(0).is(DisxLacquerBlock.itemRegistration.get())) {
                            DisxSystemMessages.noMediaFound(player);
                        }
                    } else {
                        if (this.items.get(0).is(DisxLacquerBlock.itemRegistration.get())) {
                            DisxSystemMessages.apiError(player);
                        }
                    }

                }
                this.processing = false;
                this.setMediaUrl("", player);
            }
        });

    }

    public void produceStampAsync(){
        BlockPos blockPos = this.getBlockPos();
        Level lvl = getLevel();
        CompletableFuture.runAsync(() -> {
            if (!DisxInternetCheck.checkInternet()){
                DisxSystemMessages.noInternetFoundStampMakerAsync(this.getLevel().getServer(), this.getBlockPos());
            } else {
                this.processing = true;
                try {
                    this.setChanged();
                    DisxWWW2PCMReference www2PCMReference = new DisxWWW2PCMReference(this.mediaUrl);
                    String title = www2PCMReference.probeTitle();
                    int duration = www2PCMReference.probeDuration();
                    DisxWWW2PCMSourceType sourceType = www2PCMReference.getService();
                    if (duration > 1800) {
                        DisxSystemMessages.badDuration(lvl.getServer());
                        this.processing = false;
                        this.mediaUrl = null;
                        this.setChanged();
                        return;
                    }
                    if (this.items.get(0).equals(ItemStack.EMPTY)){
                        this.processing = false;
                        this.mediaUrl = null;
                        this.setChanged();
                        return;
                    }
                    this.setItem(0, ItemStack.EMPTY);
                    lvl.playSound(null, blockPos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS);
                    DisxRecordStamp stamp = (DisxRecordStamp) DisxMain.REGISTRAR_MANAGER.get().get(Registries.ITEM).get(new ResourceLocation("disx", "record_stamp"));
                    CompoundTag tag = new CompoundTag();
                    tag.putString("mediaUrl", this.mediaUrl);
                    tag.putString("mediaName", title);
                    ItemStack stampStack = new ItemStack(stamp);
                    stampStack.setTag(tag);
                    stampStack.setCount(1);
                    if (lvl.getBlockState(blockPos.below()).is(Blocks.HOPPER)){
                        DisxLogger.debug("Found hopper below, placing record stamp in appropriate inventory for taking");
                        this.items.set(1, stampStack);
                        this.mediaUrl = null;
                        lvl.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS);
                    } else {
                        DisxLogger.debug("Did not find hopper below; Ejecting record stamp");
                        DisxLogger.debug("Below is: " + lvl.getBlockState(blockPos.below()));
                        DisxLogger.debug("BlockPos of Below: " + blockPos.below());
                        ItemEntity itemEntity = new ItemEntity(getLevel(), ((double) blockPos.getX()) + 0.5, ((double) blockPos.getY()) + 0.2, ((double) blockPos.getZ()) + 0.5, stampStack);
                        itemEntity.setDefaultPickUpDelay();
                        this.mediaUrl = null;
                        lvl.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS);
                        level.addFreshEntity(itemEntity);
                    }
                } catch (DisxMalformedURLException e){
                    DisxLogger.error(e);
                    if (this.items.get(0).is(DisxLacquerBlock.itemRegistration.get())){
                        DisxSystemMessages.badURLStampMakerAsync(lvl.getServer(), blockPos);
                    }
                } catch (DisxWWW2PCMException e){
                    DisxLogger.error(e);
                    if (e.wasResourceNotFound()){
                        if (this.items.get(0).is(DisxLacquerBlock.itemRegistration.get())) {
                            DisxSystemMessages.mediaNotFoundStampMakerAsync(lvl.getServer(), blockPos);
                        }
                    } else {
                        if (this.items.get(0).is(DisxLacquerBlock.itemRegistration.get())) {
                            DisxSystemMessages.apiErrorStampMakerAsync(lvl.getServer(), blockPos);
                        }
                    }

                }
                this.processing = false;
                this.setChanged();
            }
        });
    }


    @Override
    public int[] getSlotsForFace(Direction direction) {
        /*
        if (direction.equals(Direction.UP)){
            int[] returnValue = new int[]{1};
            return returnValue;
        }
         */
        ArrayList<Object> sideDirections = new ArrayList<>();
        sideDirections.add(Direction.NORTH);
        sideDirections.add(Direction.EAST);
        sideDirections.add(Direction.SOUTH);
        sideDirections.add(Direction.WEST);
        for (Object o : sideDirections){
            if (direction.equals(o)){
                int[] returnValue = new int[]{0};
                return returnValue;
            }
        }
        if (direction.equals(Direction.DOWN)){
            int[] returnValue = new int[]{1};
            return returnValue;
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        ArrayList<Object> sideDirections = new ArrayList<>();
        sideDirections.add(Direction.NORTH);
        sideDirections.add(Direction.EAST);
        sideDirections.add(Direction.SOUTH);
        sideDirections.add(Direction.WEST);
        for (Object o : sideDirections){
            if (direction.equals(o)){
                if (itemStack.getItem().equals(DisxLacquerBlock.itemRegistration.get())){
                    if (this.items.get(0).equals(ItemStack.EMPTY)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        if (direction.equals(Direction.DOWN) && itemStack.getItem().equals(DisxRecordStamp.getItemRegistration().get())){
            return true;
        }
        return false;
    }

    @Override
    public void clearContent() {
        this.items = NonNullList.withSize(2, ItemStack.EMPTY);
    }

    public boolean isProcessing(){
        return this.processing;
    }
}
