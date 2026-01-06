package net.WWGS.dontfreeze.compat.minecolonies.block;

import net.WWGS.dontfreeze.DontFreeze;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

@EventBusSubscriber(modid = DontFreeze.MODID)
public final class MineColoniesBlockEntityCompat
{
    private static final ResourceKey<net.minecraft.world.level.block.entity.BlockEntityType<?>> COLONY_BUILDING_BE =
            ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("minecolonies", "colonybuilding"));

    private MineColoniesBlockEntityCompat() {}

    @SubscribeEvent
    public static void onBlockEntityValidBlocks(BlockEntityTypeAddBlocksEvent event)
    {
        // minecolonies:colonybuilding BE가 우리 hut 블록을 허용하도록 추가
        event.modify(COLONY_BUILDING_BE, DontFreeze.HUT_GENERATOR.get());
    }
}