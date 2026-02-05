package net.WWGS.dontfreeze.core.block.hut;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.WWGS.dontfreeze.api.colony.building.DFBuildings;
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
        return DFBuildings.generator;
    }
}
