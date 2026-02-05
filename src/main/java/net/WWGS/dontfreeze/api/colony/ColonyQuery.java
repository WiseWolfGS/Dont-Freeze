package net.WWGS.dontfreeze.api.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface ColonyQuery {
    BlockPos findNearestTownHallPos(ServerLevel level, BlockPos pos);

    BlockPos getTownHallPosById(ServerLevel level, int colonyId);

    default Integer findColonyIdAtPos(ServerLevel level, BlockPos pos) {
        return null;
    }
}
