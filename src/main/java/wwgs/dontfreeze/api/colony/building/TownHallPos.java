package wwgs.dontfreeze.api.colony.building;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.items.component.BuildingId;
import com.minecolonies.api.items.component.ColonyId;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TownHallPos extends BuildingPos {
    public TownHallPos(@NotNull CompoundTag tag) {
        super(tag);
    }

    public TownHallPos(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    public TownHallPos(@NotNull ColonyId colonyId, @NotNull BuildingId buildingId) {
        super(colonyId, buildingId);
    }

    public TownHallPos(@NotNull ResourceKey<Level> dimensionId, int colonyId, @NotNull BlockPos buildingId) {
        super(dimensionId, colonyId, buildingId);
    }

    public TownHallPos(@NotNull IColony colony, @NotNull BlockPos buildingId) {
        super(colony, buildingId);
    }

    public TownHallPos(@NotNull IBuilding building) {
        super(building);
    }

    public TownHallPos(@NotNull IBuildingView buildingView) {
        super(buildingView);
    }
}
