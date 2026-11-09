package xyz.ar06.disx.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import xyz.ar06.disx.DisxAudioMotionType;
import xyz.ar06.disx.DisxServerAudioRegistry;
import xyz.ar06.disx.audio_filters.DisxAudioFilterType;
import xyz.ar06.disx.enchantments.DisxDiscEnchantmentHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class DisxSculkAdvancedJukeboxInventoryManager extends SavedData {
    private final HashMap<ItemStack, ItemStack> inventories = new HashMap<>();

    public boolean setDiscInventory(ItemStack frequencyStack, ItemStack discStack, Level level){
        inventories.put(frequencyStack.copy(), discStack.copy());
        CompoundTag stackTag = discStack.getTag();
        String mediaUrl = stackTag.getString("mediaUrl");
        ArrayList<DisxAudioFilterType> audioFilters = DisxDiscEnchantmentHelper.buildAudioFilterArray(discStack);
        DisxServerAudioRegistry.addToRegistry(new BlockPos(0, 0, 0), mediaUrl, null, level.getServer(), level.dimension(), false, DisxAudioMotionType.INSYNC, new UUID(0L, 0L), -1, audioFilters, frequencyStack.copy());
        setDirty();
        return true;
    }

    public ItemStack getDiscInventory(ItemStack frequencyStack){
        ItemStack key = findFrequencyKey(frequencyStack);
        return inventories.getOrDefault(key, ItemStack.EMPTY);
    }

    private ItemStack findFrequencyKey(ItemStack frequencyStack) {
        for (ItemStack key : inventories.keySet()) {
            if (ItemStack.isSameItemSameTags(key, frequencyStack)) {
                return key;
            }
        }

        return null;
    }

    public ItemStack removeDiscFromInventory(ItemStack frequencyStack, Level level){
        ItemStack key = findFrequencyKey(frequencyStack);
        ItemStack returnStack = inventories.get(key);
        inventories.remove(key);
        DisxServerAudioRegistry.removeFromRegistry(new BlockPos(0, 0, 0), level.dimension(), new UUID(0L, 0L), DisxAudioMotionType.INSYNC, frequencyStack.copy());
        setDirty();
        return returnStack;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        ListTag listTag = new ListTag();

        for (HashMap.Entry<ItemStack, ItemStack> entry : inventories.entrySet()) {
            CompoundTag entryTag = new CompoundTag();

            entryTag.put("frequency", entry.getKey().save(new CompoundTag()));
            entryTag.put("disc", entry.getValue().save(new CompoundTag()));

            listTag.add(entryTag);
        }

        compoundTag.put("inventories", listTag);

        return compoundTag;
    }

    public static DisxSculkAdvancedJukeboxInventoryManager load(CompoundTag compoundTag) {
        DisxSculkAdvancedJukeboxInventoryManager manager =
                new DisxSculkAdvancedJukeboxInventoryManager();

        ListTag listTag = compoundTag.getList("inventories", Tag.TAG_COMPOUND);

        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag entryTag = listTag.getCompound(i);

            ItemStack frequency = ItemStack.of(entryTag.getCompound("frequency"));
            ItemStack disc = ItemStack.of(entryTag.getCompound("disc"));

            manager.inventories.put(frequency, disc);
        }

        return manager;
    }

    public static DisxSculkAdvancedJukeboxInventoryManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DisxSculkAdvancedJukeboxInventoryManager::load,
                DisxSculkAdvancedJukeboxInventoryManager::new,
                "disx_sculk_jukebox_inventories"
        );
    }

}
