package net.WWGS.dontfreeze.domain.fuel.tick;

import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.domain.heat.model.ChunkHeatRef;
import net.WWGS.dontfreeze.domain.heat.model.HeatParams;
import net.WWGS.dontfreeze.domain.heat.service.ChunkHeatCache;
import net.WWGS.dontfreeze.domain.heat.storage.ColonyHeatStorage;
import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.WWGS.dontfreeze.domain.fuel.storage.ColonyFuelStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * 눈/얼음 녹이기 티커 (저부하 버전)
 *
 * B안:
 * - 큐 구성 자체를 "온도(ChunkWarmthModifier)와 동일한 청크 판정(predicate)"로 한다.
 * - radiusChunks / 타운홀 중심 반경 계산은 제거한다.
 * - (권장) 실제 처리 직전에도 재검증하여 연료/보너스 변화에 즉시 반응한다.
 */
@EventBusSubscriber
public final class SnowMeltTicker {

    /** 서버 틱당 실제 블록 변경 최대 개수 (너무 높이면 스파이크 발생) */
    private static final int MAX_MELT_PER_TICK = 40;

    /** 서버 틱당 (x,z) 컬럼 스캔 최대 개수 (너무 높이면 CPU 증가) */
    private static final int MAX_COLUMNS_PER_TICK = 800;

    /** 큐에 담을 청크 수 상한(폭주 방지) */
    private static final int MAX_CHUNKS_IN_QUEUE = 10_000;

    private static final Queue<ChunkPos> CHUNK_QUEUE = new ArrayDeque<>();
    private static ChunkPos currentChunk = null;
    private static int curIndexInChunk = 0; // 0..255 (x,z) 컬럼 인덱스

    private SnowMeltTicker() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (level == null) return;

        if (currentChunk == null && CHUNK_QUEUE.isEmpty()) {
            rebuildWorkQueue(level);
        }

        processWork(level);
    }

    /**
     * B안: "온도 시스템과 동일하게 Heated로 판정되는 청크"만 큐에 담는다.
     *
     * 구현:
     * - 연료가 있는 콜로니들의 claimed 청크를 훑되,
     * - 각 청크 cp에 대해 isHeatedNow(level, ..., cp) == true 인 경우만 추가.
     *
     * 주의:
     * - claimed 전수는 비쌀 수 있으니 MAX_CHUNKS_IN_QUEUE로 제한.
     * - Set으로 unique 보장(여러 colony 루프에서 중복 방지).
     */
    private static void rebuildWorkQueue(ServerLevel level) {
        DontFreeze.LOGGER.info("[SnowMeltTicker] rebuildWorkQueue() [B]");

        CHUNK_QUEUE.clear();
        currentChunk = null;
        curIndexInChunk = 0;

        ColonyFuelStorage fuelStorage = ColonyFuelStorage.get(level);
        ColonyHeatStorage heatStorage = ColonyHeatStorage.get(level);
        ChunkHeatCache heatCache = ChunkHeatCache.get(level);

        Set<ChunkPos> unique = new HashSet<>();

        // 연료가 있는 콜로니만 대상으로 삼되,
        // 실제로 청크를 넣을지는 isHeatedNow(온도 predicate)가 결정한다.
        for (int colonyId : fuelStorage.getColoniesWithFuel()) {
            if (fuelStorage.getFuel(colonyId) <= 0) continue;

            // (옵션) bonus<=0이면 이 콜로니는 어떤 청크도 Heated가 될 수 없으니 빠르게 스킵
            HeatParams p = heatStorage.getParams(colonyId);
            if (Math.max(0.0, p.bonus()) <= 0.0) continue;

            // claimed 전수 순회
            Set<ChunkPos> claimed = MineColoniesCompat.getClaimedChunksForColony(level, colonyId);
            if (claimed == null || claimed.isEmpty()) continue;

            for (ChunkPos cp : claimed) {
                // ✅ 온도와 동일 predicate
                if (!isHeatedNow(level, heatCache, fuelStorage, heatStorage, cp)) continue;

                unique.add(cp);
                if (unique.size() >= MAX_CHUNKS_IN_QUEUE) break;
            }

            if (unique.size() >= MAX_CHUNKS_IN_QUEUE) break;
        }

        CHUNK_QUEUE.addAll(unique);

        DontFreeze.LOGGER.info("[SnowMeltTicker] queued chunks = {}", CHUNK_QUEUE.size());
    }

    private static void processWork(ServerLevel level) {
        int melted = 0;
        int scannedColumns = 0;

        ColonyFuelStorage fuelStorage = ColonyFuelStorage.get(level);
        ColonyHeatStorage heatStorage = ColonyHeatStorage.get(level);
        ChunkHeatCache heatCache = ChunkHeatCache.get(level);

        while (melted < MAX_MELT_PER_TICK && scannedColumns < MAX_COLUMNS_PER_TICK) {

            if (currentChunk == null) {
                currentChunk = CHUNK_QUEUE.poll();
                curIndexInChunk = 0;

                if (currentChunk == null) return; // 할 일 끝
            }

            if (!level.hasChunk(currentChunk.x, currentChunk.z)) {
                currentChunk = null;
                continue;
            }

            // 권장: 처리 직전에도 재검증(연료/보너스가 변하면 즉시 반영)
            if (!isHeatedNow(level, heatCache, fuelStorage, heatStorage, currentChunk)) {
                currentChunk = null;
                continue;
            }

            int localX = curIndexInChunk & 15;          // 0..15
            int localZ = (curIndexInChunk >> 4) & 15;   // 0..15

            int x = currentChunk.getMinBlockX() + localX;
            int z = currentChunk.getMinBlockZ() + localZ;

            scannedColumns++;

            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            if (y < level.getMinBuildHeight()) {
                advanceColumnOrChunk();
                continue;
            }

            BlockPos base = new BlockPos(x, y, z);

            melted += tryMeltAt(level, base);
            if (melted < MAX_MELT_PER_TICK) {
                melted += tryMeltAt(level, base.above());
            }

            advanceColumnOrChunk();
        }
    }

    private static void advanceColumnOrChunk() {
        curIndexInChunk++;
        if (curIndexInChunk >= 256) { // 16*16 컬럼 끝
            currentChunk = null;
            curIndexInChunk = 0;
        }
    }

    private static int tryMeltAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return 0;

        // BlockEntity 있는 블록은 절대 건드리지 않는다
        if (state.hasBlockEntity()) return 0;

        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return 1;
        }

        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
            return 1;
        }

        return 0;
    }

    /**
     * ✅ ChunkWarmthModifier와 동일한 청크 판정(predicate)
     *
     * - ref = ChunkHeatCache.getOrCompute(level, cp)
     * - ref.valid
     * - claimedBy(ref.colonyId)
     * - fuel(ref.colonyId) > 0
     * - bonus(ref.colonyId) > 0
     *
     * (참고) radiusChunks는 온도 쪽에서 안 쓰므로 여기에도 넣지 않는다.
     */
    private static boolean isHeatedNow(ServerLevel level,
                                       ChunkHeatCache heatCache,
                                       ColonyFuelStorage fuelStorage,
                                       ColonyHeatStorage heatStorage,
                                       ChunkPos cp) {
        ChunkHeatRef ref = heatCache.getOrCompute(level, cp);
        if (!ref.isValid()) return false;

        int colonyId = ref.colonyId();

        if (!MineColoniesCompat.isChunkClaimedByColony(level, cp, colonyId)) return false;
        if (fuelStorage.getFuel(colonyId) <= 0) return false;

        HeatParams params = heatStorage.getParams(colonyId);
        return Math.max(0.0, params.bonus()) > 0.0;
    }
}
