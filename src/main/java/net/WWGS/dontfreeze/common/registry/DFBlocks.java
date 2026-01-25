package net.WWGS.dontfreeze.common.registry;

import net.WWGS.dontfreeze.DFConstants;
import net.WWGS.dontfreeze.common.block.BlockGeneratorCore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DFBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DFConstants.MODID);
    public static final DeferredBlock<Block> BLOCK_GENERATOR_CORE = BLOCKS.register(
            "generator_core",
            () -> new BlockGeneratorCore(
                    BlockBehaviour.Properties.of()
                            .lightLevel(state -> 15)
                            .strength(2.0f)
            )
    );

    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
