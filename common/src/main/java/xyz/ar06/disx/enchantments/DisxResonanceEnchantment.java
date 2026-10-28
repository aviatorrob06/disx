package xyz.ar06.disx.enchantments;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import xyz.ar06.disx.DisxCreativeModeTabAssembly;

public class DisxResonanceEnchantment extends Enchantment {
    public static RegistrySupplier<Enchantment> enchantmentRegistration;

    protected DisxResonanceEnchantment(Rarity rarity, EnchantmentCategory enchantmentCategory, EquipmentSlot[] equipmentSlots) {
        super(rarity, enchantmentCategory, equipmentSlots);
    }

    public static void registerEnchantment(Registrar<Enchantment> enchantmentRegistrar){
        enchantmentRegistration = enchantmentRegistrar.register(
                new ResourceLocation("disx","resonance_curse"),
                () -> new DisxResonanceEnchantment(Rarity.UNCOMMON, EnchantmentCategory.FISHING_ROD, new EquipmentSlot[]{})
        );
    }

    public static void registerTabItems(){
        CreativeTabRegistry.appendBuiltinStack(
                DisxCreativeModeTabAssembly.CREATIVE_MODE_TAB.get(),
                EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantmentRegistration.get(), 1))
        );
    }


}
