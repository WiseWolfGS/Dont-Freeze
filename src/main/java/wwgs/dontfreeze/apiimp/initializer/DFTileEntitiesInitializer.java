package wwgs.dontfreeze.apiimp.initializer;

import com.mojang.datafixers.DSL;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.api.colony.building.DFBuildings;
import wwgs.dontfreeze.api.tileentities.DFTileEntities;
import wwgs.dontfreeze.api.tileentities.DFTileEntityColonyBuildings;

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