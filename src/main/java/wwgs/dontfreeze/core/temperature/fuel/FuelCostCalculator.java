package wwgs.dontfreeze.core.temperature.fuel;

import net.minecraft.server.level.ServerLevel;

public class FuelCostCalculator
{
    public static int compute(ServerLevel level, int colonyId, double heatBonus)
    {
        int base = 20;

        double modifier = 1.0 - heatBonus;

        if (modifier < 0.1)
        {
            modifier = 0.1;
        }

        return (int)(base * modifier);
    }
}
