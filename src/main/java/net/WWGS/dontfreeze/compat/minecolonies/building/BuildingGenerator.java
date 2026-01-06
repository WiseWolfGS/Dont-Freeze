package net.WWGS.dontfreeze.compat.minecolonies.building;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;

public class BuildingGenerator extends AbstractBuilding
{
    public BuildingGenerator(final IColony colony, final BlockPos location)
    {
        super(colony, location);
    }

    @Override
    public String getSchematicName() {
        return "generator";
    }
}
