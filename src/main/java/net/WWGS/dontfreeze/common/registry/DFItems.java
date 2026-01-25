package net.WWGS.dontfreeze.common.registry;

import net.WWGS.dontfreeze.DFConstants;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DFItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DFConstants.MODID);
    public static final DeferredItem<BlockItem> ITEM_GENERATOR_CORE = ITEMS.registerSimpleBlockItem("generator_core", DFBlocks.BLOCK_GENERATOR_CORE);

    public static void init(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
