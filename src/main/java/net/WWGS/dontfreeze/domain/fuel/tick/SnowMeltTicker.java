package net.WWGS.dontfreeze.domain.heat.tick;

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
 * 핵심:
 * - 한 틱에 너무 많은 setBlock을 하지 않도록 budget을 둔다.
 * - 청크를 큐로 돌리며 조금씩 처리한다.
 * - BlockEntity가 있는 블록은 절대 건드리지 않는다 (MineColonies/Structurize 크래시 방지).
 *
 * 처리 대상:
 * - SNOW, SNOW_BLOCK -> AIR
 * - ICE, FROSTED_ICE -> WATER
 *
 * 기본적으로 PACKED_ICE, BLUE_ICE는 건드리지 않는다(지형 파괴 방지).
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

        // 진행 중에는 큐를 리셋하지 않는다.
        // 큐가 완전히 비었을 때만 “현재 활성 콜로니들” 기준으로 새 작업을 구성한다.
        if (currentChunk == null && CHUNK_QUEUE.isEmpty()) {
            rebuildWorkQueue(level);
        }

        processWork(level);
    }

    private static void rebuildWorkQueue(ServerLevel level) {
        CHUNK_QUEUE.clear();
        currentChunk = null;
        curIndexInChunk = 0;

        ColonyFuelStorage storage = ColonyFuelStorage.get(level);

        Set<ChunkPos> unique = new HashSet<>();

        for (int colonyId : storage.getColoniesWithFuel()) {
            if (storage.getFuel(colonyId) <= 0) continue;

            Set<ChunkPos> claimed = MineColoniesCompat.getClaimedChunksForColony(level, colonyId);
            if (claimed == null || claimed.isEmpty()) continue;

            unique.addAll(claimed);

            if (unique.size() >= MAX_CHUNKS_IN_QUEUE) break;
        }

        CHUNK_QUEUE.addAll(unique);
    }

    private static void processWork(ServerLevel level) {
        int melted = 0;
        int scannedColumns = 0;

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

        // ✅ BlockEntity 있는 블록은 절대 건드리지 않는다 (MineColonies/Structurize 안전)
        if (state.hasBlockEntity()) return 0;

        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
            // flags=2 : 이웃 업데이트/렌더 갱신 최소화(스파이크 감소)
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return 1;
        }

        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
            return 1;
        }

        return 0;
    }
}
