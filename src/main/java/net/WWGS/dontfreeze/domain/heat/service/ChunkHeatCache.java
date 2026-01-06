package net.WWGS.dontfreeze.domain.heat.service;

import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.WWGS.dontfreeze.domain.heat.model.ChunkHeatRef;
import net.WWGS.dontfreeze.domain.heat.storage.ColonyHeatStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkHeatCache {
    private static final Map<ServerLevel, ChunkHeatCache> PER_LEVEL = new WeakHashMap<>();

    public static ChunkHeatCache get(ServerLevel level) {
        synchronized (PER_LEVEL) {
            return PER_LEVEL.computeIfAbsent(level, __ -> new ChunkHeatCache());
        }
    }

    private final Map<Long, ChunkHeatRef> cache = new ConcurrentHashMap<>();

    private ChunkHeatCache() {}

    public ChunkHeatRef getOrCompute(ServerLevel level, ChunkPos cp) {
        long key = cp.toLong();
        long now = level.getGameTime();

        ChunkHeatRef cur = cache.get(key);
        // 기본: 20틱(= 1초)마다만 재탐색
        int ttlTicks = 20;
        if (cur != null && (now - cur.updatedGameTime()) < ttlTicks) {
            return cur;
        }

        // 청크 중심 기준으로 "가장 가까운 타운홀" 탐색
        // y는 탐색에 의미가 없게 만드는 게 좋음(대부분 xz 기반이니까).
        BlockPos chunkCenter = new BlockPos(cp.getMinBlockX() + 8, level.getSeaLevel(), cp.getMinBlockZ() + 8);

        BlockPos townHall = MineColoniesCompat.findNearestTownHall(level, chunkCenter);
        if (townHall == null) {
            ChunkHeatRef none = ChunkHeatRef.none(now);
            cache.put(key, none);
            return none;
        }

        Integer colonyId = MineColoniesCompat.getColonyIdFromTownHall(level, townHall);
        if (colonyId == null) {
            ChunkHeatRef none = ChunkHeatRef.none(now);
            cache.put(key, none);
            return none;
        }

        ChunkHeatRef ref = new ChunkHeatRef(colonyId, townHall, now);
        if (ref.isValid()) {
            ColonyHeatStorage.get(level).setTownHallPos(ref.colonyId(), ref.townHallPos());
        }
        cache.put(key, ref);
        return ref;
    }

    public void invalidate(ChunkPos cp) {
        cache.remove(cp.toLong());
    }

    public void clear() {
        cache.clear();
    }
}
