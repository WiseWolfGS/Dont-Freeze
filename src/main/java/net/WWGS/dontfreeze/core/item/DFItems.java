package net.WWGS.dontfreeze.core.item;

import net.WWGS.dontfreeze.Dontfreeze;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DFItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Dontfreeze.MODID);

    public static final DeferredItem<Item> ITEM_GENERATOR_CORE = ITEMS.registerItem("generator_core", Item::new, new Item.Properties());
    public static final DeferredItem<Item> ITEM_HUT_GENERATOR = ITEMS.registerItem("blockhutgenerator", Item::new, new Item.Properties());

    public static void init(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
