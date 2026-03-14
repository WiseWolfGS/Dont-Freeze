package wwgs.dontfreeze.core.temperature.fuel;

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
 * Safely collects building count / total level / per-type stats for a MineColonies colony.
 *
 * Reflection fallbacks are kept because MineColonies API signatures can drift by version.
 */
public final class ColonyBuildingStats
{
    private ColonyBuildingStats()
    {
    }

    public record Stats(
            int buildingCount,
            int totalLevelSum,
            @NotNull Map<ResourceLocation, Integer> countByType,
            @NotNull Map<ResourceLocation, Integer> levelSumByType
    )
    {
        public static final Stats ZERO = new Stats(0, 0, Map.of(), Map.of());
    }

    public static @NotNull Stats collect(@NotNull ServerLevel level, int colonyId)
    {
        if (colonyId <= 0)
        {
            return Stats.ZERO;
        }

        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null)
        {
            return Stats.ZERO;
        }

        final IColony colony;
        try
        {
            colony = manager.getColonyByWorld(colonyId, level);
        }
        catch (Throwable ignored)
        {
            return Stats.ZERO;
        }

        if (colony == null)
        {
            return Stats.ZERO;
        }

        final Map<?, ?> buildings;
        try
        {
            buildings = colony.getServerBuildingManager().getBuildings();
        }
        catch (Throwable ignored)
        {
            return Stats.ZERO;
        }

        if (buildings == null || buildings.isEmpty())
        {
            return Stats.ZERO;
        }

        int count = 0;
        int levelSum = 0;

        Map<ResourceLocation, Integer> countByType = new HashMap<>();
        Map<ResourceLocation, Integer> levelSumByType = new HashMap<>();

        for (Object value : buildings.values())
        {
            if (!(value instanceof IBuilding building))
            {
                continue;
            }

            count++;

            Integer levelValue = getBuildingLevelSafe(building);
            int safeLevel = levelValue == null ? 0 : Math.max(0, levelValue);
            levelSum += safeLevel;

            ResourceLocation typeId = getBuildingTypeIdSafe(building);
            if (typeId != null)
            {
                countByType.merge(typeId, 1, Integer::sum);
                levelSumByType.merge(typeId, safeLevel, Integer::sum);
            }
        }

        return new Stats(
                count,
                levelSum,
                Map.copyOf(countByType),
                Map.copyOf(levelSumByType)
        );
    }

    /**
     * Prefer MineColonies building type registry id if available.
     * Fallback to schematic name as minecolonies:<schematic>.
     */
    private static @Nullable ResourceLocation getBuildingTypeIdSafe(@NotNull IBuilding building)
    {
        try
        {
            Object type = building.getClass().getMethod("getBuildingType").invoke(building);
            if (type != null)
            {
                ResourceLocation registryName = tryGetRegistryName(type);
                if (registryName != null)
                {
                    return registryName;
                }
            }
        }
        catch (Throwable ignored)
        {
        }

        try
        {
            String schematic = building.getSchematicName();
            if (schematic != null && !schematic.isBlank())
            {
                return ResourceLocation.fromNamespaceAndPath("minecolonies", schematic.trim());
            }
        }
        catch (Throwable ignored)
        {
        }

        return null;
    }

    private static @Nullable ResourceLocation tryGetRegistryName(@NotNull Object type)
    {
        try
        {
            Object value = type.getClass().getMethod("getRegistryName").invoke(type);
            if (value instanceof ResourceLocation rl)
            {
                return rl;
            }
        }
        catch (Throwable ignored)
        {
        }

        try
        {
            var field = type.getClass().getDeclaredField("registryName");
            field.setAccessible(true);
            Object value = field.get(type);
            if (value instanceof ResourceLocation rl)
            {
                return rl;
            }
        }
        catch (Throwable ignored)
        {
        }

        return null;
    }

    private static @Nullable Integer getBuildingLevelSafe(@NotNull IBuilding building)
    {
        try
        {
            return building.getBuildingLevel();
        }
        catch (Throwable ignored)
        {
        }

        Object value = invokeNoArgs(building, "getBuildingLevel");
        if (value instanceof Integer level)
        {
            return level;
        }

        value = invokeNoArgs(building, "getLevel");
        if (value instanceof Integer level)
        {
            return level;
        }

        return null;
    }

    private static @Nullable Object invokeNoArgs(@NotNull Object target, @NotNull String methodName)
    {
        try
        {
            var method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        }
        catch (Throwable ignored)
        {
            return null;
        }
    }
}