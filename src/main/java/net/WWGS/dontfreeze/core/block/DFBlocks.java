package net.WWGS.dontfreeze.core.block;

import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.block.hut.BlockHutGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DFBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Dontfreeze.MODID);

    public static final DeferredHolder<Block, BlockHutGenerator> BLOCK_HUT_GENERATOR =
            BLOCKS.register(
                    "hut_generator",
                    () -> new BlockHutGenerator(BlockBehaviour.Properties.of()
                            .strength(2.0F)
                            .noOcclusion()
                    ));
    public static final DeferredHolder<Block, Block> BLOCK_GENERATOR_CORE =
            BLOCKS.register(
                "generator_core",
                () -> new BlockGeneratorCore(
                        BlockBehaviour.Properties.of()
                                .lightLevel(state -> 15)
                                .strength(2.0f)
                ));

    public static void init(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
