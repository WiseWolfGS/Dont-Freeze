package net.WWGS.dontfreeze.core.common.item;

import net.WWGS.dontfreeze.Dontfreeze;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class DFCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dontfreeze.MODID);

    public static final Supplier<CreativeModeTab> DONTFREEZE_ITEMS_TAB = CREATIVE_MODE_TAB.register(
        "dontfreeze_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(DFItems.ITEM_GENERATOR_CORE.get()))
                    .title(Component.translatable("creativetab.dontfreeze.dontfreeze_items"))
                    .displayItems(
                            (itemDisplayParameters, output) -> {
                                output.accept(DFItems.ITEM_GENERATOR_CORE);
                                output.accept(DFItems.ITEM_HUT_GENERATOR);
                            }
                    ).build()
    );

    public static void init(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
