package net.WWGS.dontfreeze.domain.heat.service;

import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.WWGS.dontfreeze.domain.fuel.storage.ColonyFuelStorage;
import net.WWGS.dontfreeze.domain.heat.model.ChunkHeatRef;
import net.WWGS.dontfreeze.domain.heat.model.HeatParams;
import net.WWGS.dontfreeze.domain.heat.storage.ColonyHeatStorage;
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

        int colonyId = ref.colonyId();

// 클레임 체크
        if (!MineColoniesCompat.isChunkClaimedByColony(level, cp, colonyId)) return false;

// 연료 체크
        if (ColonyFuelStorage.get(level).getFuel(colonyId) <= 0) return false;

// bonus 체크
        HeatParams params = ColonyHeatStorage.get(level).getParams(colonyId);
        if (params.bonus() <= 0) return false;

// radius 체크 (선택)
        BlockPos townHall = ColonyHeatStorage.get(level).getTownHallPos(colonyId);
        if (townHall != null && params.radiusChunks() > 0) {
            ChunkPos center = new ChunkPos(townHall);
            if (Math.abs(cp.x - center.x) > params.radiusChunks()
                    || Math.abs(cp.z - center.z) > params.radiusChunks()) {
                return false;
            }
        }

        return true;

    }
}
