package xyz.ar06.disx.items;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xyz.ar06.disx.DisxCreativeModeTabAssembly;
import xyz.ar06.disx.DisxMain;

public class DisxVinylShard extends Item {

    public DisxVinylShard(Properties properties) {
        super(properties);
    }

    public static RegistrySupplier<Item> itemRegistration;

    public static void registerItem(Registrar<Item> itemRegistry){
        itemRegistration = itemRegistry.register(new ResourceLocation("disx", "vinyl_shard"), () -> new DisxBlankDisc(new Item.Properties().stacksTo(64)));

    }

    public static void registerTabItem(){
        CreativeTabRegistry.appendBuiltinStack(
                DisxCreativeModeTabAssembly.CREATIVE_MODE_TAB.get(),
                new ItemStack(itemRegistration.get())
        );
    }
}
