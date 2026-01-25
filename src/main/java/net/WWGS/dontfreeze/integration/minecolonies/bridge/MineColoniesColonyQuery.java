package net.WWGS.dontfreeze.integration.minecolonies.bridge;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.claim.IChunkClaimData;
import net.WWGS.dontfreeze.common.api.colony.ColonyQuery;
import net.WWGS.dontfreeze.common.api.colony.TownHallRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;

public final class MineColoniesColonyQuery implements ColonyQuery {
    @Override
    public @Nullable TownHallRef findNearestTownHall(ServerLevel level, BlockPos pos) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return null;

        IColony colony = manager.getClosestIColony(level, pos);
        TownHallRef ref = toTownHallRef(colony);
        if (ref != null) return ref;

        return null;
    }

    @Override
    public @Nullable BlockPos getTownHallPos(ServerLevel level, int colonyId) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return null;

        IColony colony = manager.getColonyByWorld(colonyId, level);
        if (colony == null) return null;

        ITownHall townHall = colony.getBuildingManager().getTownHall();
        if (townHall == null) return null;

        return townHall.getPosition();
    }

    private static @Nullable TownHallRef toTownHallRef(@Nullable IColony colony) {
        if (colony == null) return null;

        ITownHall townHall = colony.getBuildingManager().getTownHall();
        if (townHall == null) return null;

        BlockPos thPos = townHall.getPosition();
        if (thPos == null) return null;

        return new TownHallRef(colony.getID(), thPos);
    }

    @Override
    public @Nullable Integer findColonyIdAt(ServerLevel level, BlockPos pos) {
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
