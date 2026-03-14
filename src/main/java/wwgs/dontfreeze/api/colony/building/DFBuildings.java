package wwgs.dontfreeze.api.colony.building;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;
import wwgs.dontfreeze.core.blocks.DFBlocks;

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
