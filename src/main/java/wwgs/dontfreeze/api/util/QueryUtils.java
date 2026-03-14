package wwgs.dontfreeze.api.util;

import wwgs.dontfreeze.api.colony.building.query.BuildingQuery;
import org.jetbrains.annotations.Nullable;
import wwgs.dontfreeze.api.colony.citizen.query.CitizenQuery;

public final class QueryUtils {
    private static @Nullable BuildingQuery buildingQuery;
    private static @Nullable CitizenQuery citizenQuery;

    public static void registerColonyQuery(BuildingQuery impl) {
        buildingQuery = impl;
    }

    public static void registerCitizenQuery(CitizenQuery impl) {
        citizenQuery = impl;
    }

    public static @Nullable BuildingQuery buildingQuery() {
        return buildingQuery;
    }

    public static @Nullable CitizenQuery citizenQuery() {
        return citizenQuery;
    }
}
