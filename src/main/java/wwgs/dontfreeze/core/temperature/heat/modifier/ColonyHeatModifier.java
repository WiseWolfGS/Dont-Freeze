package wwgs.dontfreeze.core.temperature.heat.modifier;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import wwgs.dontfreeze.api.colony.building.TownHallPos;
import wwgs.dontfreeze.api.colony.building.query.BuildingQuery;
import wwgs.dontfreeze.api.util.QueryUtils;
import wwgs.dontfreeze.core.temperature.fuel.FuelSavedData;
import wwgs.dontfreeze.core.temperature.heat.HeatBonusSavedData;

import java.util.function.Function;

public final class ColonyHeatModifier extends TempModifier
{
    private static final double MAX_TEMP = 1.0;

    @Override
    protected Function<Double, Double> calculate(LivingEntity livingEntity, Temperature.Trait trait)
    {
        if (!(livingEntity.level() instanceof ServerLevel level))
        {
            return Function.identity();
        }

        if (!level.dimension().equals(Level.OVERWORLD))
        {
            return Function.identity();
        }

        BuildingQuery buildingQuery = QueryUtils.buildingQuery();
        if (buildingQuery == null)
        {
            return Function.identity();
        }

        BlockPos pos = livingEntity.blockPosition();
        TownHallPos townHallPos = buildingQuery.findNearestTownHallPos(level, pos);
        if (townHallPos == null)
        {
            return Function.identity();
        }

        int colonyId = townHallPos.getColonyId();
        if (colonyId <= 0)
        {
            return Function.identity();
        }

        if (FuelSavedData.get(level).getOrCreate(colonyId).getStored() <= 0)
        {
            return Function.identity();
        }

        double bonus = Math.max(0.0, HeatBonusSavedData.get(level).getBonus(colonyId));
        if (bonus <= 0.0)
        {
            return Function.identity();
        }

        return temp -> {
            if (temp >= MAX_TEMP)
            {
                return temp;
            }

            double allowed = MAX_TEMP - temp;
            double applied = Math.min(bonus, allowed);
            return temp + applied;
        };
    }
}