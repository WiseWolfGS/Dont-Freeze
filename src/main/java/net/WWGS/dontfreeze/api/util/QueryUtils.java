package net.WWGS.dontfreeze.api.util;

import net.WWGS.dontfreeze.api.colony.ColonyQuery;
import net.WWGS.dontfreeze.api.colony.GeneratorQuery;
import org.jetbrains.annotations.Nullable;

public final class QueryUtils {
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
