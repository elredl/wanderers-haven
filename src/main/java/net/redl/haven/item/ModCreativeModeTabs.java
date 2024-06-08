package net.redl.haven.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.redl.haven.HavenMain;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HavenMain.MOD_ID);

    public static final RegistryObject<CreativeModeTab> HAVEN_INGREDIENTS = CREATIVE_MODE_TABS.register("haven_ingredients",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.SILVER.get()))
                    .title(Component.translatable("creativetab.haven_ingredients"))
                    .displayItems(((pParameters, pOutput) -> {
                        // PAUSING HERE
                    }))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
