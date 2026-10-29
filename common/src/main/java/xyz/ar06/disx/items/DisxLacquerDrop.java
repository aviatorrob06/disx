package xyz.ar06.disx.items;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.fuel.FuelRegistry;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xyz.ar06.disx.DisxCreativeModeTabAssembly;

public class DisxLacquerDrop extends Item {
    public DisxLacquerDrop(Properties properties) {
        super(properties);
    }

    public static RegistrySupplier<Item> itemRegistration;

    public static void registerItem(Registrar<Item> registrar){
        itemRegistration = registrar.register(
                new ResourceLocation("disx","lacquer_drop"),
                () -> new DisxLacquerDrop(
                        new Item.Properties()
                )
        );
        //FuelRegistry.register(100, itemRegistration.get());
    }

    public static void registerTabItem(){
        CreativeTabRegistry.appendBuiltinStack(
                DisxCreativeModeTabAssembly.CREATIVE_MODE_TAB.get(),
                new ItemStack(itemRegistration.get())
        );
    }
}
