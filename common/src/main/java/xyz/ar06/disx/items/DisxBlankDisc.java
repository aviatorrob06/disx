package xyz.ar06.disx.items;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xyz.ar06.disx.DisxCreativeModeTabAssembly;

public class DisxBlankDisc extends Item{

    public static RegistrySupplier<Item> itemRegistration;
    public DisxBlankDisc(Properties properties) {
        super(properties);
    }

    public static void registerItem(Registrar<Item> itemRegistry){
        itemRegistration = itemRegistry.register(new ResourceLocation("disx", "blank_disc"), () -> new DisxBlankDisc(new Item.Properties().stacksTo(64)));
    }

    public static void registerTabItem(){
        CreativeTabRegistry.appendBuiltinStack(
                DisxCreativeModeTabAssembly.CREATIVE_MODE_TAB.get(),
                new ItemStack(itemRegistration.get())
        );
    }
}
