package net.WWGS.dontfreeze.core.colony.fuel.service;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Safely collects "building count" and "sum of building levels" for a MineColonies colony.
 *
 * We keep reflection fallbacks because MineColonies API signatures can drift by version.
 */
public final class ColonyBuildingStats {
    private ColonyBuildingStats() {}

    public record Stats(
            int buildingCount,
            int totalLevelSum,
            @NotNull Map<ResourceLocation, Integer> countByType,
            @NotNull Map<ResourceLocation, Integer> levelSumByType
    ) {
        public static final Stats ZERO = new Stats(0, 0, Map.of(), Map.of());
    }

    public static @NotNull Stats collect(@NotNull ServerLevel level, int colonyId) {
        if (colonyId <= 0) return Stats.ZERO;

        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return Stats.ZERO;

        final IColony colony;
        try {
            colony = manager.getColonyByWorld(colonyId, level);
        } catch (Throwable t) {
            return Stats.ZERO;
        }
        if (colony == null) return Stats.ZERO;

        final Map<?, ?> buildings;
        try {
            buildings = colony.getServerBuildingManager().getBuildings();
        } catch (Throwable t) {
            return Stats.ZERO;
        }
        if (buildings == null || buildings.isEmpty()) return Stats.ZERO;

        int count = 0;
        int levelSum = 0;

        Map<ResourceLocation, Integer> countByType = new HashMap<>();
        Map<ResourceLocation, Integer> levelSumByType = new HashMap<>();

        for (Object v : buildings.values()) {
            if (!(v instanceof IBuilding b)) continue;
            count++;

            Integer lvl = getBuildingLevelSafe(b);
            if (lvl != null) levelSum += Math.max(0, lvl);

            ResourceLocation typeId = getBuildingTypeIdSafe(b);
            if (typeId != null) {
                countByType.merge(typeId, 1, Integer::sum);
                if (lvl != null) levelSumByType.merge(typeId, Math.max(0, lvl), Integer::sum);
            }
        }

        return new Stats(count, levelSum, Map.copyOf(countByType), Map.copyOf(levelSumByType));
    }

    /**
     * Attempts to extract a MineColonies building type registry name.
     *
     * We prefer the building type's registry name (ResourceLocation), since it's stable and matches entries like
     * minecolonies:builder, minecolonies:barracks, etc.
     */
    private static @Nullable ResourceLocation getBuildingTypeIdSafe(@NotNull IBuilding b) {
        // 1) Try getBuildingType().getRegistryName()
        try {
            Object type = b.getClass().getMethod("getBuildingType").invoke(b);
            if (type != null) {
                ResourceLocation rl = tryGetRegistryName(type);
                if (rl != null) return rl;
            }
        } catch (Throwable ignored) {
        }

        // 2) Fallback: schematic name (usually matches ModBuildings constants like "builder")
        try {
            String schematic = b.getSchematicName();
            if (schematic != null && !schematic.isBlank()) {
                // Interpret as minecolonies:<path>
                return ResourceLocation.fromNamespaceAndPath("minecolonies", schematic.trim());
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static @Nullable ResourceLocation tryGetRegistryName(@NotNull Object type) {
        try {
            Object o = type.getClass().getMethod("getRegistryName").invoke(type);
            if (o instanceof ResourceLocation rl) return rl;
        } catch (Throwable ignored) {
        }

        try {
            var f = type.getClass().getDeclaredField("registryName");
            f.setAccessible(true);
            Object o = f.get(type);
            if (o instanceof ResourceLocation rl) return rl;
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static @Nullable Integer getBuildingLevelSafe(@NotNull IBuilding b) {
        // 1) Direct call if present
        try {
            return b.getBuildingLevel();
        } catch (Throwable ignored) {
        }

        // 2) Reflection fallbacks
        Object v = invokeNoArgs(b, "getBuildingLevel");
        if (v instanceof Integer i) return i;

        v = invokeNoArgs(b, "getLevel");
        if (v instanceof Integer i) return i;

        return null;
    }

    private static @Nullable Object invokeNoArgs(@NotNull Object target, @NotNull String method) {
        try {
            var m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
