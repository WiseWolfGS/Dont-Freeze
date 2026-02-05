package net.WWGS.dontfreeze.core.colony.fuel.platform;

import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.block.DFBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Dontfreeze.MODID)
public final class GeneratorCorePlacementListener {
    private GeneratorCorePlacementListener() {}

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent e) {
        Level level = (Level) e.getLevel();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel sl)) return;

        if (e.getPlacedBlock().is(DFBlocks.BLOCK_GENERATOR_CORE.get())) {
            BlockPos pos = e.getPos();
            GeneratorCoreRegistry.get(sl).add(pos);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent e) {
        Level level = (Level) e.getLevel();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel sl)) return;

        if (e.getState().is(DFBlocks.BLOCK_GENERATOR_CORE.get())) {
            GeneratorCoreRegistry.get(sl).remove(e.getPos());
        }
    }
}
