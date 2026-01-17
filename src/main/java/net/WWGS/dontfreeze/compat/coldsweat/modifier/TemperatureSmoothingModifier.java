package net.WWGS.dontfreeze.compat.coldsweat.modifier;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import net.WWGS.dontfreeze.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class TemperatureSmoothingModifier extends TempModifier
{
    private static final String TAG_KEY = "dontfreeze:temp_smoothing_last_core";

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        if (trait != Temperature.Trait.CORE)
            return temp -> temp;

        double m = Config.tempChangeMultiplier; // 1.0=기본, 0.1=10배 느림
        if (Double.isNaN(m) || Double.isInfinite(m)) m = 1.0;
        m = Math.max(0.0, m);

        final double multiplier = m;
        final CompoundTag data = entity.getPersistentData();

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
