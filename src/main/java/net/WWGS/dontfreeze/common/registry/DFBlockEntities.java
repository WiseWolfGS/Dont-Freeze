package net.WWGS.dontfreeze.common.registry;

import net.WWGS.dontfreeze.DFConstants;
import net.WWGS.dontfreeze.common.block.entity.BlockGeneratorCoreEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DFBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DFConstants.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorCoreEntity>> BLOCK_GENERATOR_CORE =
            BLOCK_ENTITIES.register("generator_core", () ->
                    BlockEntityType.Builder.of(
                            BlockGeneratorCoreEntity::new,
                            DFBlocks.BLOCK_GENERATOR_CORE.get()
                    ).build(null)
            );

    public static void init(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}

