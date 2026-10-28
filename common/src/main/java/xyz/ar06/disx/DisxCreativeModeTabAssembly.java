package xyz.ar06.disx;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import xyz.ar06.disx.blocks.*;
import xyz.ar06.disx.enchantments.DisxResonanceEnchantment;
import xyz.ar06.disx.enchantments.DisxRetrogradeCurseEnchantment;
import xyz.ar06.disx.enchantments.DisxTempoEnchantment;
import xyz.ar06.disx.items.DisxAdvancedJukeboxMinecartItem;
import xyz.ar06.disx.items.DisxBlankDisc;
import xyz.ar06.disx.items.DisxLacquerDrop;
import xyz.ar06.disx.items.DisxVinylShard;

public class DisxCreativeModeTabAssembly {
    public static RegistrySupplier<CreativeModeTab> CREATIVE_MODE_TAB;

    public static void addItems(Minecraft minecraft) {
        DisxAdvancedJukebox.registerTabItem();
        DisxEnderAdvancedJukebox.registerTabItem();
        DisxAdvancedJukeboxMinecartItem.registerTabItem();

        DisxLacquerBlock.registerTabItem();
        DisxLacquerDrop.registerTabItem();
        DisxStampMaker.registerTabItem();

        DisxVinylShard.registerTabItem();
        DisxBlankDisc.registerTabItem();
        DisxRecordPress.registerTabItem();

        DisxResonanceEnchantment.registerTabItems();
        DisxTempoEnchantment.registerTabItems();
        DisxRetrogradeCurseEnchantment.registerTabItems();
    }
}
