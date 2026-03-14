package wwgs.dontfreeze.api.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import wwgs.dontfreeze.api.colony.building.TownHallPos;
import wwgs.dontfreeze.api.colony.building.query.BuildingQuery;
import wwgs.dontfreeze.core.temperature.fuel.FuelSavedData;
import wwgs.dontfreeze.core.temperature.heat.HeatBonusSavedData;

public class HeatedUtils {
    public static boolean isHeatedNow(ServerLevel level, BlockPos pos) {
        BuildingQuery cq = QueryUtils.buildingQuery();
        if (cq == null) return false;

        TownHallPos townHall = QueryUtils.buildingQuery().findNearestTownHallPos(level, pos);
        int colonyId = townHall.getColonyId();
        if (colonyId <= 0) return false;

        if (FuelSavedData.get(level).getOrCreate(colonyId).getStored() <= 0) return false;

        double bonus = HeatBonusSavedData.get(level).getBonus(colonyId);

        return bonus > 0.0;
    }
}
