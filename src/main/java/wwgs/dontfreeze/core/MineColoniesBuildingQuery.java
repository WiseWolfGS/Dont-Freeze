package wwgs.dontfreeze.core;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.claim.IChunkClaimData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import wwgs.dontfreeze.api.colony.building.BuildingPos;
import wwgs.dontfreeze.api.colony.building.TownHallPos;
import wwgs.dontfreeze.api.colony.building.query.BuildingQuery;
import wwgs.dontfreeze.core.blocks.GeneratorCoreRegistry;

public class MineColoniesBuildingQuery implements BuildingQuery
{
    @Override
    public @Nullable TownHallPos findNearestTownHallPos(ServerLevel level, BlockPos pos)
    {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null)
        {
            return null;
        }

        Integer colonyId = findColonyIdAtPos(manager, level, pos);
        if (colonyId == null)
        {
            return null;
        }

        return getTownHallPos(level, colonyId);
    }

    @Override
    public @Nullable TownHallPos getTownHallPos(ServerLevel level, int colonyId)
    {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null)
        {
            return null;
        }

        IColony colony = manager.getColonyByWorld(colonyId, level);
        if (colony == null)
        {
            return null;
        }

        return toTownHallPos(colony);
    }

    @Override
    public @Nullable Integer getColonyIdByTownHall(ServerLevel level, TownHallPos townHallPos)
    {
        int colonyId = townHallPos.getColonyId();
        return colonyId > 0 ? colonyId : null;
    }

    @Override
    public @Nullable BuildingPos getGeneratorPos(ServerLevel level, int colonyId)
    {
        TownHallPos townHall = getTownHallPos(level, colonyId);
        if (townHall == null)
        {
            return null;
        }

        BlockPos best = null;
        double bestDist2 = Double.MAX_VALUE;

        for (BlockPos corePos : GeneratorCoreRegistry.get(level).getAll())
        {
            Integer owner = findColonyIdAtPos(IColonyManager.getInstance(), level, corePos);
            if (owner == null || owner != colonyId)
            {
                continue;
            }

            double dist2 = corePos.distSqr(townHall.getBuildingId());
            if (dist2 < bestDist2)
            {
                bestDist2 = dist2;
                best = corePos.immutable();
            }
        }

        return best == null ? null : new BuildingPos(level.dimension(), colonyId, best);
    }

    private static @Nullable TownHallPos toTownHallPos(@Nullable IColony colony)
    {
        if (colony == null)
        {
            return null;
        }

        ITownHall townHall = colony.getServerBuildingManager().getTownHall();
        if (townHall == null)
        {
            return null;
        }

        return new TownHallPos(colony, townHall.getPosition());
    }

    private static @Nullable Integer findColonyIdAtPos(IColonyManager manager, ServerLevel level, BlockPos pos)
    {
        return owningColony(manager, level, new ChunkPos(pos));
    }

    private static @Nullable Integer owningColony(IColonyManager manager, ServerLevel level, ChunkPos chunkPos)
    {
        try
        {
            IChunkClaimData data = manager.getClaimData(level.dimension(), chunkPos);
            if (data == null)
            {
                return null;
            }

            int colonyId = data.getOwningColony();
            return colonyId > 0 ? colonyId : null;
        }
        catch (Throwable ignored)
        {
            return null;
        }
    }
}