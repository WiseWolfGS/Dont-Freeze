package net.WWGS.dontfreeze.common.api.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public interface ColonyQuery {
    @Nullable
    TownHallRef findNearestTownHall(ServerLevel level, BlockPos pos);

    @Nullable
    BlockPos getTownHallPos(ServerLevel level, int colonyId);

    @Nullable
    default Integer findColonyIdAt(ServerLevel level, BlockPos pos) {
        return null;
    }
}
