package wwgs.dontfreeze.api.colony.building.query;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import wwgs.dontfreeze.api.colony.building.BuildingPos;
import wwgs.dontfreeze.api.colony.building.TownHallPos;

public interface BuildingQuery {
    TownHallPos findNearestTownHallPos(ServerLevel level, BlockPos pos);
    TownHallPos getTownHallPos(ServerLevel level, int colonyId);
    default Integer getColonyIdByTownHall(ServerLevel level, TownHallPos townHallPos) {
        return null;
    }

    BuildingPos getGeneratorPos(ServerLevel level, int colonyId);
}
