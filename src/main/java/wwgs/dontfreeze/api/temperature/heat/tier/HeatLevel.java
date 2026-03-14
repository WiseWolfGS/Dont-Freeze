package wwgs.dontfreeze.api.temperature.heat.tier;

public enum HeatLevel
{
    FREEZING,
    COLD,
    STABLE,
    WARM,
    OVERHEATED;

    public static HeatLevel fromRatio(double ratio)
    {
        if (ratio <= 0.10D)
        {
            return FREEZING;
        }
        if (ratio <= 0.35D)
        {
            return COLD;
        }
        if (ratio <= 0.70D)
        {
            return STABLE;
        }
        if (ratio <= 0.95D)
        {
            return WARM;
        }

        return OVERHEATED;
    }
}