package net.WWGS.dontfreeze.core.colony;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.WWGS.dontfreeze.api.colony.GeneratorQuery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MineColoniesGeneratorQuery implements GeneratorQuery {

    @Override
    public Set<BlockPos> getGeneratorHutPositions(ServerLevel level, int colonyId) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return Collections.emptySet();

        IColony colony = manager.getColonyByWorld(colonyId, level);
        if (colony == null) return Collections.emptySet();

        Map<BlockPos, IBuilding> buildings = colony.getBuildingManager().getBuildings();
        if (buildings.isEmpty()) return Collections.emptySet();

        Set<BlockPos> out = new HashSet<>();

        for (IBuilding b : buildings.values()) {
            if (b == null) continue;

            if (matchesByBuildingTypeRegistryName(b)) {
                out.add(b.getPosition());
                continue;
            }

            try {
                if ("generator".equals(b.getSchematicName())) {
                    out.add(b.getPosition());
                }
            } catch (Throwable ignored) {
            }
        }

        return out;
    }

    private static boolean matchesByBuildingTypeRegistryName(IBuilding b) {
        try {
            Object type = b.getClass().getMethod("getBuildingType").invoke(b);
            if (type == null) return false;

            ResourceLocation rl = tryGetRegistryName(type);
            if (rl == null) return false;

            return "generator".equals(rl.getPath());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ResourceLocation tryGetRegistryName(Object type) {
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
}
