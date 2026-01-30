package net.WWGS.dontfreeze.core.common.block;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.WWGS.dontfreeze.core.common.building.DFBuildings;
import org.jetbrains.annotations.NotNull;

public class BlockHutGenerator extends AbstractBlockHut<BlockHutGenerator> {

    public BlockHutGenerator(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull String getHutName() {
        return "generator";
    }
    @Override
    public BuildingEntry getBuildingEntry() {
        return DFBuildings.BUILDING_GENERATOR.get();
    }
}
