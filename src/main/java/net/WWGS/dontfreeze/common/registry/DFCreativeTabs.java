package net.WWGS.dontfreeze.common.registry;

import net.WWGS.dontfreeze.DFConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DFCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DFConstants.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DF_TAB =
            TABS.register("dontfreeze", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dontfreeze"))
                    .displayItems((params, output) -> {
                        output.accept(DFItems.ITEM_GENERATOR_CORE.get());
                    })
                    .build()
            );

    public static void init(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
