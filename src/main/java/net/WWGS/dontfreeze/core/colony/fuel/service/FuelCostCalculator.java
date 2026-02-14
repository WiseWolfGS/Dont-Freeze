package net.WWGS.dontfreeze.core.colony.fuel.service;

import net.WWGS.dontfreeze.DFConfig;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.*;

/**
 * Single source of truth for "fuel cost per second" calculation.
 *
 * Fuel is stored as "burn ticks" (vanilla furnace burn time units).
 * We consume "cost" once per second (see GeneratorSystemTicker PERIOD_TICKS=20).
 */
public final class FuelCostCalculator {
    private FuelCostCalculator() {}

    public record CostBreakdown(
            int totalCostPerSecond,
            int baseCost,
            int heatBonusCost,
            int buildingCountCost,
            int buildingLevelCost,
            int buildingCount,
            int buildingLevelSum
    ) {}

    public static @NotNull CostBreakdown compute(@NotNull ServerLevel level, int colonyId, double heatBonus) {
        int baseCost = max(0, DFConfig.baseCostOfCore);
        int heatCost = max(0, (int) ceil(heatBonus * DFConfig.bonusCostOfCore));

        ColonyBuildingStats.Stats stats = ColonyBuildingStats.collect(level, colonyId);

        // Building-driven cost supports per-building-type overrides via config.
        double countExtra = 0.0;
        for (var e : stats.countByType().entrySet()) {
            var tc = DFConfig.getBuildingTypeCost(e.getKey());
            if (tc != null && tc.countCost() > 0.0) {
                countExtra += e.getValue() * tc.countCost();
            }
        }

        double levelExtra = 0.0;
        for (var e : stats.levelSumByType().entrySet()) {
            var tc = DFConfig.getBuildingTypeCost(e.getKey());
            if (tc != null && tc.levelCost() > 0.0) {
                levelExtra += e.getValue() * tc.levelCost();
            }
        }

        int bCountCost = max(0, (int) ceil(countExtra));
        int bLevelCost = max(0, (int) ceil(levelExtra));

        int total = baseCost + heatCost + bCountCost + bLevelCost;
        // Safety: never allow 0 or negative cost in running system.
        total = max(1, total);

        return new CostBreakdown(
                total,
                baseCost,
                heatCost,
                bCountCost,
                bLevelCost,
                stats.buildingCount(),
                stats.totalLevelSum()
        );
    }
}
