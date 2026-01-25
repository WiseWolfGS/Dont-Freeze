package net.WWGS.dontfreeze.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DFConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    static final ModConfigSpec.DoubleValue TEMP_CHANGE_MULTIPLIER;
    static final ModConfigSpec.DoubleValue CORE_MAX_HEAT;
    static final ModConfigSpec.DoubleValue CORE_MIN_HEAT;
    static final ModConfigSpec.IntValue MAX_FUEL_TICKS;
    static final ModConfigSpec.IntValue BASE_COST_OF_CORE;
    static final ModConfigSpec.IntValue BONUS_COST_OF_CORE;

    static {
        BUILDER.push("temperature");
        TEMP_CHANGE_MULTIPLIER = BUILDER
                .comment(
                        "Multiplier for core temperature change rate",
                        "1.0 = vanilla Cold Sweat behavior",
                        "0.5 = temperature changes 2x slower",
                        "2.0 = temperature changes 2x faster"
                )
                .defineInRange(
                        "tempChangeMultiplier",
                        1.0,
                        0.0,
                        2.0
                );
        BUILDER.pop();

        BUILDER.push("heat");
        CORE_MAX_HEAT = BUILDER
                .comment(
                        "Max heat from the generator core"
                )
                .defineInRange(
                        "coreMaxHeat",
                        5.0,
                        4.0,
                        6.0
                );
        CORE_MIN_HEAT = BUILDER
                .comment(
                        "Min heat from the generator core"
                )
                .defineInRange(
                        "coreMinHeat",
                        2.0,
                        0.0,
                        4.0
                );
        BUILDER.pop();

        BUILDER.push("fuel");
        MAX_FUEL_TICKS = BUILDER
                .comment(
                        "Max tick of core's fuel storage"
                )
                .defineInRange(
                        "maxFuelTicks",
                        7200000,
                        0,
                        Integer.MAX_VALUE
                );
        BASE_COST_OF_CORE = BUILDER
                .comment(
                        "Base cost of active core"
                )
                .defineInRange(
                        "baseCostOfCore",
                        10,
                        1,
                        72000
                );
        BONUS_COST_OF_CORE = BUILDER
                .comment(
                        "Bonus cost of active core"
                )
                .defineInRange(
                        "bonusCostOfCore",
                        5,
                        1,
                        72000
                );
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
