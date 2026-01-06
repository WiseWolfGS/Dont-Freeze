package net.WWGS.dontfreeze.domain.heat.service;

import net.WWGS.dontfreeze.domain.fuel.storage.ColonyFuelStorage;
import net.WWGS.dontfreeze.domain.heat.model.ChunkHeatRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class HeatedArea {
    private HeatedArea() {}

    /** pos가 속한 청크가 "유효한 히터 범위"이고 연료가 남아있으면 true */
    public static boolean isHeated(ServerLevel level, BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        ChunkHeatRef ref = ChunkHeatCache.get(level).getOrCompute(level, cp);
        if (!ref.isValid()) return false;

        long fuel = ColonyFuelStorage.get(level).getFuel(ref.colonyId());
        return fuel > 0;
    }
}
