package net.WWGS.dontfreeze;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = Dontfreeze.MODID)
public class DFConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    private static final Supplier<List<? extends String>> defaultSupplier = () -> List.of("minecraft:coals");

    static final ModConfigSpec.DoubleValue TEMP_CHANGE_MULTIPLIER;
    static final ModConfigSpec.DoubleValue CORE_MAX_HEAT;
    static final ModConfigSpec.DoubleValue CORE_MIN_HEAT;
    static final ModConfigSpec.IntValue MAX_FUEL_TICKS;
    static final ModConfigSpec.IntValue BASE_COST_OF_CORE;
    static final ModConfigSpec.IntValue BONUS_COST_OF_CORE;
    static final ModConfigSpec.ConfigValue<List<? extends String>> FUEL_ITEM_STRINGS;

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
        FUEL_ITEM_STRINGS = BUILDER
                .comment(
                        "A list of items for generator fuel"
                )
                .defineInList(
                        "items",
                        defaultSupplier,
                        new ArrayList<>(Collections.singletonList(defaultSupplier.get()))
                );
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static double tempChangeMultiplier;
    public static double coreMaxHeat;
    public static double coreMinHeat;
    public static int maxFuelTicks;
    public static int baseCostOfCore;
    public static int bonusCostOfCore;
    public static Set<Item> fuelItems;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        tempChangeMultiplier = TEMP_CHANGE_MULTIPLIER.get();
        coreMaxHeat = CORE_MAX_HEAT.get();
        coreMinHeat = CORE_MIN_HEAT.get();
        maxFuelTicks = MAX_FUEL_TICKS.get();
        baseCostOfCore = BASE_COST_OF_CORE.get();
        bonusCostOfCore = BONUS_COST_OF_CORE.get();

        fuelItems = FUEL_ITEM_STRINGS.get().stream().map(itemName -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName))).collect(Collectors.toSet());
    }
}
