package net.WWGS.dontfreeze.core.colony.heat.platform;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.WWGS.dontfreeze.DFConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public final class WorldHeatSmoothingModifier extends TempModifier {
    private static final String TAG_KEY = "dontfreeze:temp_smoothing_last_core";

    @Override
    protected Function<Double, Double> calculate(LivingEntity livingEntity, Temperature.Trait trait) {
        if (trait != Temperature.Trait.CORE)
            return temp -> temp;

        double multiplier;

        if (Double.isNaN(DFConfig.tempChangeMultiplier) || Double.isInfinite(DFConfig.tempChangeMultiplier)) {
            multiplier = Math.max(0.0, DFConfig.tempChangeMultiplier);
        }
        else {
            multiplier = 1.0;
        }

        final CompoundTag data = livingEntity.getPersistentData();

        return targetTemp ->
        {
            if (!data.contains(TAG_KEY))
            {
                data.putDouble(TAG_KEY, targetTemp);
                return targetTemp;
            }

            double last = data.getDouble(TAG_KEY);
            double next = last + (targetTemp - last) * multiplier;
            data.putDouble(TAG_KEY, next);
            return next;
        };
    }
}
