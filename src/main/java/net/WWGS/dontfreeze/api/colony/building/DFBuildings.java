package net.WWGS.dontfreeze.api.colony.building;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.WWGS.dontfreeze.core.block.DFBlocks;
import org.jetbrains.annotations.NotNull;

public class DFBuildings {
    public static final String GENERATOR_ID = "generator";

    public static BuildingEntry generator;

    private DFBuildings() {
        throw new IllegalStateException("Tried to initialize: DFBuildings but this is a Utility class.");
    }

    @NotNull
    public static AbstractBlockHut<?>[] getHuts() {
        return new AbstractBlockHut[] {
                DFBlocks.BLOCK_HUT_GENERATOR.get(),
        };
    }
}
