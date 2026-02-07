package net.WWGS.dontfreeze.apiimp.initializer;

import com.mojang.datafixers.DSL;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.api.colony.building.DFBuildings;
import net.WWGS.dontfreeze.api.tileentities.DFTileEntities;
import net.WWGS.dontfreeze.api.tileentities.DFTileEntityColonyBuildings;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DFTileEntitiesInitializer {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Dontfreeze.MODID);

    static {
        DFTileEntities.BUILDING = BLOCK_ENTITIES.register("colonybuilding",
                () -> BlockEntityType.Builder
                        .of(DFTileEntityColonyBuildings::new, DFBuildings.getHuts())
                        .build(DSL.remainderType())
        );
    }
}
