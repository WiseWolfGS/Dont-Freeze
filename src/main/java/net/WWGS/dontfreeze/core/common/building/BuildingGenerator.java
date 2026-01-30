package net.WWGS.dontfreeze.core.common.building;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class BuildingGenerator extends AbstractBuilding {
    public static final String GENERATOR = "generator";

    protected BuildingGenerator(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    @NotNull
    @Override
    public String getSchematicName() {
        return GENERATOR;
    }
}
