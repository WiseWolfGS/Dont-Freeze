package net.WWGS.dontfreeze.compat.minecolonies.building;

import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.core.colony.buildings.views.AbstractBuildingView;
import net.minecraft.core.BlockPos;

public class BuildingGeneratorView extends AbstractBuildingView
{
    public BuildingGeneratorView(final IColonyView colony, final BlockPos location)
    {
        super(colony, location);
    }
}
