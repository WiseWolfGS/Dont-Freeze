package wwgs.dontfreeze.core.temperature.fuel;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import wwgs.dontfreeze.Config;
import wwgs.dontfreeze.api.colony.building.BuildingPos;
import wwgs.dontfreeze.api.colony.building.query.BuildingQuery;
import wwgs.dontfreeze.api.util.QueryUtils;

import static java.lang.Math.ceil;
import static java.lang.Math.max;

public final class FuelCostCalculator
{
    private static final ResourceLocation HEAT_FUEL_EFFECT =
            ResourceLocation.fromNamespaceAndPath("dontfreeze", "effects/heat_fuel_multiplier");

    private static final Logger LOGGER = LogUtils.getLogger();

    private FuelCostCalculator()
    {
    }

    public record CostBreakdown(
            int totalCostPerSecond,
            int baseCost,
            int heatBonusCost,
            int buildingCountCost,
            int buildingLevelCost,
            int buildingCount,
            int buildingLevelSum,
            int generatorLevel,
            double researchMultiplier
    )
    {
    }

    public static int compute(ServerLevel level, int colonyId, double heatBonus)
    {
        return computeBreakdown(level, colonyId, heatBonus).totalCostPerSecond();
    }

    public static @NotNull CostBreakdown computeBreakdown(@NotNull ServerLevel level, int colonyId, double heatBonus)
    {
        int baseCost = max(0, Config.baseCostOfCore);
        double multiplier = 1.0;
        int generatorLevel = 1;

        try
        {
            IColonyManager manager = IColonyManager.getInstance();
            if (manager != null)
            {
                IColony colony = manager.getColonyByWorld(colonyId, level);
                if (colony != null && colony.getResearchManager() != null)
                {
                    double strength = colony.getResearchManager()
                            .getResearchEffects()
                            .getEffectStrength(HEAT_FUEL_EFFECT);

                    if (strength > 0.0)
                    {
                        multiplier = Math.max(0.0, 1.0 - strength);
                    }

                    BuildingQuery buildingQuery = QueryUtils.buildingQuery();
                    if (buildingQuery != null)
                    {
                        BuildingPos generatorPos = buildingQuery.getGeneratorPos(level, colonyId);
                        if (generatorPos != null)
                        {
                            var building = colony.getServerBuildingManager().getBuilding(generatorPos.getBuildingId());
                            if (building != null)
                            {
                                generatorLevel = Math.max(1, building.getBuildingLevel());
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to read colony research/generator level (colonyId={})", colonyId, e);
        }

        int heatCost = max(0, (int) ceil(heatBonus * Config.bonusCostOfCore * multiplier));

        ColonyBuildingStats.Stats stats = ColonyBuildingStats.collect(level, colonyId);

        double countExtra = 0.0;
        for (var entry : stats.countByType().entrySet())
        {
            var typeCost = Config.getBuildingTypeCost(entry.getKey());
            if (typeCost.countCost() > 0.0)
            {
                countExtra += entry.getValue() * typeCost.countCost();
            }
        }

        double levelExtra = 0.0;
        for (var entry : stats.levelSumByType().entrySet())
        {
            var typeCost = Config.getBuildingTypeCost(entry.getKey());
            if (typeCost.levelCost() > 0.0)
            {
                levelExtra += entry.getValue() * typeCost.levelCost();
            }
        }

        int buildingCountCost = Math.max(0, (int) Math.ceil(countExtra));
        int buildingLevelCost = Math.max(0, (int) Math.ceil(levelExtra));

        int total = baseCost
                + heatCost
                + buildingCountCost
                + buildingLevelCost
                - 2 * generatorLevel;

        total = max(1, total);

        return new CostBreakdown(
                total,
                baseCost,
                heatCost,
                buildingCountCost,
                buildingLevelCost,
                stats.buildingCount(),
                stats.totalLevelSum(),
                generatorLevel,
                multiplier
        );
    }
}
