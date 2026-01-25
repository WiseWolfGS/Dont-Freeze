package net.WWGS.dontfreeze.common.config;

import net.neoforged.fml.event.config.ModConfigEvent;

public final class DFConfigValues {
    public static double tempChangeMultiplier;
    public static double coreMaxHeat;
    public static double coreMinHeat;
    public static int maxFuelTicks;
    public static int baseCostOfCore;
    public static int bonusCostOfCore;

    public static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != DFConfig.SPEC) {
            return;
        }

        tempChangeMultiplier = DFConfig.TEMP_CHANGE_MULTIPLIER.get();
        coreMaxHeat = DFConfig.CORE_MAX_HEAT.get();
        coreMinHeat = DFConfig.CORE_MIN_HEAT.get();
        maxFuelTicks = DFConfig.MAX_FUEL_TICKS.get();
        baseCostOfCore = DFConfig.BASE_COST_OF_CORE.get();
        bonusCostOfCore = DFConfig.BONUS_COST_OF_CORE.get();
    }
}
