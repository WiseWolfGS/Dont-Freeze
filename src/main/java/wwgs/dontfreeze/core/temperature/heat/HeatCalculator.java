package wwgs.dontfreeze.core.temperature.heat;

import net.minecraft.server.level.ServerLevel;

public final class HeatCalculator
{
    private HeatCalculator() {}

    public static int computeHeatPerTick(ServerLevel level, int colonyId)
    {
        int baseHeat = 10;

        // TODO:
        // - generator level
        // - building bonus
        // - research bonus
        // - weather penalty
        // - citizen count scaling
        return baseHeat;
    }

    public static int computeHeatLossPerTick(ServerLevel level, int colonyId)
    {
        int baseLoss = 1;

        // TODO:
        // - biome/weather difficulty
        // - colony size
        // - insulation bonus
        return baseLoss;
    }
}