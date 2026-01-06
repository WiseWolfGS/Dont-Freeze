package net.WWGS.dontfreeze.domain.heat.model;

import net.minecraft.core.BlockPos;

public record ChunkHeatRef(
        int colonyId,
        BlockPos townHallPos,
        long updatedGameTime
) {
    public static ChunkHeatRef none(long gameTime) {
        return new ChunkHeatRef(-1, null, gameTime);
    }

    public boolean isValid() {
        return colonyId >= 0 && townHallPos != null;
    }
}