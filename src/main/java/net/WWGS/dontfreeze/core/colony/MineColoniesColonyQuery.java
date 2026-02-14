package net.WWGS.dontfreeze.core.colony;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.claim.IChunkClaimData;
import net.WWGS.dontfreeze.api.colony.ColonyQuery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MineColoniesColonyQuery implements ColonyQuery {

    @Override
    public @Nullable BlockPos findNearestTownHallPos(ServerLevel level, BlockPos pos) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return null;

        try {
            IColony colony = manager.getClosestIColony(level, pos);
            BlockPos townHallPos = toTownHallPos(colony);
            if (townHallPos != null) return townHallPos;
        } catch (Throwable ignored) {}

        try {
            ResourceKey<Level> dim = level.dimension();
            Map<ChunkPos, IChunkClaimData> claimMap = manager.getClaimData(dim);
            if (claimMap == null || claimMap.isEmpty()) return null;

            Set<Integer> colonyIds = new HashSet<>();
            for (IChunkClaimData data : claimMap.values()) {
                if (data == null) continue;
                int id = data.getOwningColony();
                if (id > 0) colonyIds.add(id);
            }
            if (colonyIds.isEmpty()) return null;

            double bestDist2 = Double.MAX_VALUE;
            BlockPos best = null;

            for (int colonyId : colonyIds) {
                BlockPos th = getTownHallPosById(level, colonyId);
                if (th == null) continue;

                double d2 = th.distSqr(pos);
                if (d2 < bestDist2) {
                    bestDist2 = d2;
                    best = th;
                }
            }
            return best;
        } catch (Throwable ignored) {}

        return null;
    }

    @Override
    public @Nullable BlockPos getTownHallPosById(ServerLevel level, int colonyId) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return null;

        IColony colony = manager.getColonyByWorld(colonyId, level);
        if (colony == null) return null;

        ITownHall townHall = colony.getServerBuildingManager().getTownHall();
        if (townHall == null) return null;

        return townHall.getPosition();
    }

    private static @Nullable BlockPos toTownHallPos(@Nullable IColony colony) {
        if (colony == null) return null;

        ITownHall townHall = colony.getServerBuildingManager().getTownHall();
        if (townHall == null) return null;

        return townHall.getPosition();
    }

    @Override
    public @Nullable Integer findColonyIdAtPos(ServerLevel level, BlockPos pos) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return null;

        ChunkPos base = new ChunkPos(pos);
        return owningColony(manager, level, base);
    }

    private static @Nullable Integer owningColony(IColonyManager manager, ServerLevel level, ChunkPos cp) {
        try {
            IChunkClaimData data = manager.getClaimData(level.dimension(), cp);
            if (data == null) return null;
            int id = data.getOwningColony();
            return (id > 0) ? id : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
