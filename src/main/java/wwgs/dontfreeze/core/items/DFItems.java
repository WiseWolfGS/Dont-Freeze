package wwgs.dontfreeze.core.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.core.blocks.DFBlocks;

public class DFItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Dontfreeze.MODID);

    public static final DeferredItem<BlockItem> ITEM_GENERATOR_CORE =
            ITEMS.register("generator_core",
                    () -> new BlockItem(DFBlocks.BLOCK_GENERATOR_CORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ITEM_HUT_GENERATOR =
            ITEMS.register("blockhutgenerator",
                    () -> new BlockItem(DFBlocks.BLOCK_HUT_GENERATOR.get(), new Item.Properties()));

    public static void init(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
