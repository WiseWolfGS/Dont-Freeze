package wwgs.dontfreeze.core.temperature.heat;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wwgs.dontfreeze.api.temperature.heat.IHeatData;
import wwgs.dontfreeze.api.temperature.heat.IHeatManager;
import wwgs.dontfreeze.api.temperature.heat.tier.HeatLevel;

public class HeatManager implements IHeatManager
{
    @Override
    public @Nullable IHeatData getHeatData(@NotNull ServerLevel level, int colonyId)
    {
        return HeatSavedData.get(level).getOrCreate(colonyId);
    }

    @Override
    public @NotNull IHeatData getOrCreateHeatData(@NotNull ServerLevel level, int colonyId)
    {
        return HeatSavedData.get(level).getOrCreate(colonyId);
    }

    @Override
    public int addHeat(@NotNull ServerLevel level, int colonyId, int amount)
    {
        return getOrCreateHeatData(level, colonyId).addHeat(amount);
    }

    @Override
    public int consumeHeat(@NotNull ServerLevel level, int colonyId, int amount)
    {
        return getOrCreateHeatData(level, colonyId).consumeHeat(amount);
    }

    @Override
    public int getStoredHeat(@NotNull ServerLevel level, int colonyId)
    {
        return getOrCreateHeatData(level, colonyId).getStored();
    }

    @Override
    public int getHeatCapacity(@NotNull ServerLevel level, int colonyId)
    {
        return getOrCreateHeatData(level, colonyId).getCapacity();
    }

    @Override
    public HeatLevel getHeatLevel(@NotNull ServerLevel level, int colonyId)
    {
        IHeatData data = getOrCreateHeatData(level, colonyId);
        int capacity = Math.max(1, data.getCapacity());
        double ratio = (double) data.getStored() / capacity;
        return HeatLevel.fromRatio(ratio);
    }

    @Override
    public void tick(@NotNull ServerLevel level, int colonyId)
    {
        IHeatData data = getOrCreateHeatData(level, colonyId);

        int gain = HeatCalculator.computeHeatPerTick(level, colonyId);
        int loss = HeatCalculator.computeHeatLossPerTick(level, colonyId);

        if (gain > 0)
        {
            data.addHeat(gain);
        }

        if (loss > 0)
        {
            data.consumeHeat(loss);
        }
    }
}