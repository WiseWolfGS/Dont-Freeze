package net.WWGS.dontfreeze.common.api;

import net.WWGS.dontfreeze.common.api.colony.ColonyQuery;
import net.WWGS.dontfreeze.common.api.generator.GeneratorQuery;

import javax.annotation.Nullable;

public class IntegrationAPIs {
    private static @Nullable ColonyQuery colonyQuery;
    private static @Nullable GeneratorQuery generatorQuery;

    public static void registerColonyQuery(ColonyQuery impl) {
        colonyQuery = impl;
    }

    public static void registerGeneratorQuery(GeneratorQuery impl) {
        generatorQuery = impl;
    }

    public static @Nullable ColonyQuery colonyQuery() {
        return colonyQuery;
    }

    public static @Nullable GeneratorQuery generatorQuery() {
        return generatorQuery;
    }
}
