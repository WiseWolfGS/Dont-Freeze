package wwgs.dontfreeze.api.util;

import wwgs.dontfreeze.api.colony.building.query.BuildingQuery;
import org.jetbrains.annotations.Nullable;

public final class QueryUtils {
    private static @Nullable BuildingQuery buildingQuery;

    public static void registerColonyQuery(BuildingQuery impl) {
        buildingQuery = impl;
    }

    public static @Nullable BuildingQuery colonyQuery() {
        return buildingQuery;
    }
}
