package net.WWGS.dontfreeze.core.colony.fuel.service;

import net.WWGS.dontfreeze.api.colony.ColonyQuery;
import net.WWGS.dontfreeze.api.util.QueryUtils;
import net.WWGS.dontfreeze.core.colony.fuel.platform.GeneratorCoreRegistry;
import net.WWGS.dontfreeze.core.colony.fuel.storage.ColonyCoreResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class NearestToTownHallCoreResolver implements ColonyCoreResolver {

    @Override
    public @Nullable BlockPos resolve(ServerLevel level, int colonyId) {
        ColonyQuery colonyQuery = QueryUtils.colonyQuery();
        if (colonyQuery == null) return null;

        BlockPos townHall = colonyQuery.getTownHallPosById(level, colonyId);
        if (townHall == null) return null;

        Set<BlockPos> candidates = GeneratorCoreRegistry.get(level).getAll();
        BlockPos best = null;
        double bestDist2 = Double.MAX_VALUE;

        for (BlockPos pos : candidates) {
            Integer cid = colonyQuery.findColonyIdAtPos(level, pos);
            if (cid == null || cid != colonyId) continue;

            double d2 = pos.distSqr(townHall);
            if (d2 < bestDist2) {
                bestDist2 = d2;
                best = pos;
            }
        }
        return best;
    }
}


