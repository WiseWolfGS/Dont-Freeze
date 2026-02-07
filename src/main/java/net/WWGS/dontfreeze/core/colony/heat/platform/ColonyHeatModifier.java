package net.WWGS.dontfreeze.core.colony.heat.platform;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.WWGS.dontfreeze.api.colony.ColonyQuery;
import net.WWGS.dontfreeze.api.util.QueryUtils;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelService;
import net.WWGS.dontfreeze.core.colony.heat.storage.ColonyHeatParamsStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public final class ColonyHeatModifier extends TempModifier {
    @Override
    protected Function<Double, Double> calculate(LivingEntity livingEntity, Temperature.Trait trait) {
        if (!(livingEntity.level() instanceof ServerLevel level)) {
            return Function.identity();
        }

        if (!level.dimension().equals(Level.OVERWORLD)) {
            return Function.identity();
        }

        ColonyQuery colonyQuery = QueryUtils.colonyQuery();
        if (colonyQuery == null) return Function.identity();

        BlockPos pos = livingEntity.blockPosition();
        Integer colonyId = colonyQuery.findColonyIdAtPos(level, pos);
        if (colonyId == null) return Function.identity();

        if(FuelService.getFuel(level, colonyId) <= 0) return Function.identity();

        var params = ColonyHeatParamsStorage.get(level).getParams(colonyId);
        double bonus = Math.max(0.0, params.bonus());
        if (bonus <= 0.0) return Function.identity();

        double maxTemp = 1.0;

        return temp -> {
            if (temp >= maxTemp) return temp;

            if (temp > 0) {
                double allowed = maxTemp - temp;
                double applied = Math.min(bonus, allowed);
                return temp + applied;
            }

            return Math.min(temp + bonus, maxTemp);
        };
    }
}
