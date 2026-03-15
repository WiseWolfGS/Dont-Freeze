package wwgs.dontfreeze.core.temperature.heat.ticker;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.claim.IChunkClaimData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.api.util.CompatUtils;
import wwgs.dontfreeze.api.util.HeatedUtils;
import wwgs.dontfreeze.core.common.weather.SRMCompat;
import wwgs.dontfreeze.core.temperature.fuel.FuelData;
import wwgs.dontfreeze.core.temperature.fuel.FuelSavedData;
import wwgs.dontfreeze.core.temperature.heat.HeatBonusSavedData;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@EventBusSubscriber(modid = Dontfreeze.MODID)
public final class MeltTicker
{
    private MeltTicker()
    {
    }

    // =========================
    // TUNING
    // =========================

    /** 틱당 실제 처리량 */
    private static final int MAX_PER_TICK = 40;

    /** 틱당 스캔 청크 수 */
    private static final int SCAN_CHUNKS_PER_TICK = 2;

    /** claimMap 갱신 주기(틱) */
    private static final int CLAIM_REFRESH_INTERVAL = 200;

    /** 확률 실패 후 재시도 딜레이 */
    private static final int RETRY_DELAY_TICKS = 60;

    /** 성공 후 재시도 쿨다운 */
    private static final int SUCCESS_COOLDOWN_TICKS = 0;

    private static final int MAX_QUEUE_SIZE = 200_000;

    // =========================
    // State
    // =========================

    /** dim -> melt queue */
    private static final Map<ResourceKey<Level>, ArrayDeque<BlockPos>> QUEUES = new HashMap<>();

    /** dim -> round-robin ring of colony chunk keys */
    private static final Map<ResourceKey<Level>, ArrayDeque<Long>> RINGS = new HashMap<>();

    /** dim -> chunkKey -> colonyId */
    private static final Map<ResourceKey<Level>, Map<Long, Integer>> CHUNK_OWNER = new HashMap<>();

    /** dim -> positions currently pending in queue (dedupe) */
    private static final Map<ResourceKey<Level>, HashSet<Long>> PENDING = new HashMap<>();

    /** dim -> posLong -> next allowed try tick */
    private static final Map<ResourceKey<Level>, Map<Long, Long>> NEXT_TRY_TICK = new HashMap<>();

    private static long tickCounter = 0L;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }

        tickCounter++;

        ResourceKey<Level> dim = level.dimension();
        ArrayDeque<BlockPos> queue = QUEUES.computeIfAbsent(dim, k -> new ArrayDeque<>());
        PENDING.computeIfAbsent(dim, k -> new HashSet<>());
        NEXT_TRY_TICK.computeIfAbsent(dim, k -> new HashMap<>());

        if (tickCounter % CLAIM_REFRESH_INTERVAL == 0)
        {
            rebuildRing(level);
        }

        scanColonyChunks(level, queue);
        processMeltQueue(level, queue);
    }

    private static void rebuildRing(ServerLevel level)
    {
        ResourceKey<Level> dim = level.dimension();

        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null)
        {
            RINGS.put(dim, new ArrayDeque<>());
            CHUNK_OWNER.put(dim, new HashMap<>());
            return;
        }

        Map<ChunkPos, IChunkClaimData> claimMap;
        try
        {
            claimMap = manager.getClaimData(dim);
        }
        catch (Throwable t)
        {
            return;
        }

        ArrayDeque<Long> ring = new ArrayDeque<>();
        Map<Long, Integer> owner = new HashMap<>();

        if (claimMap != null)
        {
            for (Map.Entry<ChunkPos, IChunkClaimData> entry : claimMap.entrySet())
            {
                ChunkPos chunkPos = entry.getKey();
                IChunkClaimData data = entry.getValue();
                if (chunkPos == null || data == null)
                {
                    continue;
                }

                int colonyId = data.getOwningColony();
                if (colonyId <= 0)
                {
                    continue;
                }

                if (!isColonyHeated(level, colonyId))
                {
                    continue;
                }

                long key = chunkPos.toLong();
                ring.addLast(key);
                owner.put(key, colonyId);
            }
        }

        RINGS.put(dim, ring);
        CHUNK_OWNER.put(dim, owner);
    }

    private static boolean isColonyHeated(ServerLevel level, int colonyId)
    {
        try
        {
            FuelData fuelData = FuelSavedData.get(level).get(colonyId);
            if (fuelData == null || fuelData.getStored() <= 0)
            {
                return false;
            }

            double bonus = HeatBonusSavedData.get(level).getBonus(colonyId);
            return bonus > 0.0;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    private static void scanColonyChunks(ServerLevel level, ArrayDeque<BlockPos> queue)
    {
        ResourceKey<Level> dim = level.dimension();
        ArrayDeque<Long> ring = RINGS.get(dim);
        if (ring == null || ring.isEmpty())
        {
            return;
        }

        Map<Long, Integer> ownerMap = CHUNK_OWNER.get(dim);
        if (ownerMap == null || ownerMap.isEmpty())
        {
            return;
        }

        for (int i = 0; i < SCAN_CHUNKS_PER_TICK; i++)
        {
            if (ring.isEmpty())
            {
                return;
            }

            long chunkKey = ring.pollFirst();
            ring.addLast(chunkKey);

            Integer colonyIdObj = ownerMap.get(chunkKey);
            if (colonyIdObj == null)
            {
                continue;
            }

            int colonyId = colonyIdObj;
            if (colonyId <= 0)
            {
                continue;
            }

            if (!isColonyHeated(level, colonyId))
            {
                continue;
            }

            ChunkPos chunkPos = new ChunkPos(chunkKey);
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            if (chunk == null)
            {
                continue;
            }

            scanChunkSurfaceOnly(level, chunk, chunkPos, queue);
        }
    }

    private static void scanChunkSurfaceOnly(ServerLevel level, LevelChunk chunk, ChunkPos chunkPos, ArrayDeque<BlockPos> queue)
    {
        ResourceKey<Level> dim = level.dimension();
        HashSet<Long> pending = PENDING.get(dim);
        if (pending == null)
        {
            return;
        }

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                int worldX = baseX + x;
                int worldZ = baseZ + z;

                int yTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);

                // SRM snow-covered block 대응:
                // 잔디/꽃/울타리/계단/슬랩 위쪽/변형 위치까지 조금 넓게 본다.
                for (int y = yTop - 2; y <= yTop + 2; y++)
                {
                    if (y < minY || y >= maxY)
                    {
                        continue;
                    }

                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (!level.isLoaded(pos))
                    {
                        continue;
                    }

                    BlockState state = chunk.getBlockState(pos);

                    if (!isTargetFrozen(level, state))
                    {
                        continue;
                    }

                    if (!HeatedUtils.isHeatedNow(level, pos))
                    {
                        continue;
                    }

                    if (queue.size() >= MAX_QUEUE_SIZE)
                    {
                        return;
                    }

                    long packed = pos.asLong();
                    if (pending.add(packed))
                    {
                        queue.addLast(pos.immutable());
                    }
                }
            }
        }
    }

    private static void processMeltQueue(ServerLevel level, ArrayDeque<BlockPos> queue)
    {
        ResourceKey<Level> dim = level.dimension();

        HashSet<Long> pending = PENDING.get(dim);
        Map<Long, Long> nextTry = NEXT_TRY_TICK.get(dim);
        if (pending == null || nextTry == null)
        {
            return;
        }

        RandomSource random = level.random;

        int processed = 0;
        while (processed++ < MAX_PER_TICK && !queue.isEmpty())
        {
            BlockPos pos = queue.pollFirst();
            long packed = pos.asLong();

            pending.remove(packed);

            if (!level.isLoaded(pos))
            {
                nextTry.remove(packed);
                continue;
            }

            if (!HeatedUtils.isHeatedNow(level, pos))
            {
                nextTry.remove(packed);
                continue;
            }

            Long nextAllowedTick = nextTry.get(packed);
            if (nextAllowedTick != null && tickCounter < nextAllowedTick)
            {
                if (pending.add(packed))
                {
                    queue.addLast(pos.immutable());
                }
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!isTargetFrozen(level, state))
            {
                nextTry.remove(packed);
                continue;
            }

            double chance = meltChance(level, state);

            if (random.nextDouble() > chance)
            {
                nextTry.put(packed, tickCounter + RETRY_DELAY_TICKS);
                if (pending.add(packed))
                {
                    queue.addLast(pos.immutable());
                }
                continue;
            }

            meltFrozenBlock(level, pos, state);

            BlockState after = level.getBlockState(pos);
            if (isTargetFrozen(level, after) && HeatedUtils.isHeatedNow(level, pos))
            {
                if (SUCCESS_COOLDOWN_TICKS > 0)
                {
                    nextTry.put(packed, tickCounter + SUCCESS_COOLDOWN_TICKS);
                }
                else
                {
                    nextTry.remove(packed);
                }

                if (pending.add(packed))
                {
                    queue.addLast(pos.immutable());
                }
            }
            else
            {
                nextTry.remove(packed);
            }
        }
    }

    private static boolean isTargetFrozen(ServerLevel level, BlockState state)
    {
        if (isVanillaFrozen(state))
        {
            return true;
        }

        return CompatUtils.hasSnowRealMagic() && SRMCompat.isSrmBlock(level, state);
    }

    private static boolean isVanillaFrozen(BlockState state)
    {
        return state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static double meltChance(ServerLevel level, BlockState state)
    {
        if (CompatUtils.hasSnowRealMagic() && SRMCompat.isSrmBlock(level, state))
        {
            return 0.12;
        }

        if (state.is(Blocks.SNOW))
        {
            return 0.10;
        }

        if (state.is(Blocks.SNOW_BLOCK))
        {
            return 0.03;
        }

        if (state.is(Blocks.POWDER_SNOW))
        {
            return 0.04;
        }

        if (state.is(Blocks.ICE))
        {
            return 0.010;
        }

        if (state.is(Blocks.PACKED_ICE))
        {
            return 0.004;
        }

        if (state.is(Blocks.BLUE_ICE))
        {
            return 0.0025;
        }

        return Mth.clamp(0.01, 0.0005, 0.20);
    }

    private static void meltFrozenBlock(ServerLevel level, BlockPos pos, BlockState state)
    {
        if (CompatUtils.hasSnowRealMagic() && SRMCompat.isSrmBlock(level, state))
        {
            meltSrmBlock(level, pos, state);
            return;
        }

        if (state.is(Blocks.SNOW))
        {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            if (layers > 1)
            {
                level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, layers - 1), 2);
            }
            else
            {
                level.removeBlock(pos, false);
            }
            return;
        }

        if (state.is(Blocks.SNOW_BLOCK))
        {
            level.setBlock(
                    pos,
                    Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 8),
                    2
            );
            return;
        }

        if (state.is(Blocks.POWDER_SNOW))
        {
            level.removeBlock(pos, false);
            return;
        }

        meltIce(level, pos);
    }

    private static void meltSrmBlock(ServerLevel level, BlockPos pos, BlockState state)
    {
        // 1) SRM 레이어 계열이면 가능하면 한 층씩 줄인다.
        Integer layers = getLayerValue(state);
        if (layers != null)
        {
            if (layers > 1)
            {
                BlockState lowered = lowerLayer(state, layers - 1);
                if (lowered != null)
                {
                    level.setBlock(pos, lowered, 2);
                    return;
                }
            }

            // 2) 마지막 단계면 원본 블록 복구 시도
            if (SRMCompat.tryRestoreOriginal(level, pos))
            {
                return;
            }

            // 3) 복구 실패 시 최후 fallback
            level.removeBlock(pos, false);
            return;
        }

        // 레이어 프로퍼티가 없는 SRM snowy variant도 원본 복구 시도
        if (SRMCompat.tryRestoreOriginal(level, pos))
        {
            return;
        }

        // 그래도 실패하면 제거
        level.removeBlock(pos, false);
    }

    private static Integer getLayerValue(BlockState state)
    {
        for (var property : state.getProperties())
        {
            if (property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty intProperty
                    && "layers".equals(intProperty.getName()))
            {
                return state.getValue(intProperty);
            }
        }
        return null;
    }

    private static BlockState lowerLayer(BlockState state, int newLayers)
    {
        for (var property : state.getProperties())
        {
            if (property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty intProperty
                    && "layers".equals(intProperty.getName()))
            {
                if (intProperty.getPossibleValues().contains(newLayers))
                {
                    return state.setValue(intProperty, newLayers);
                }
            }
        }
        return null;
    }

    private static void meltIce(ServerLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);

        if (!state.is(Blocks.ICE) && !state.is(Blocks.PACKED_ICE) && !state.is(Blocks.BLUE_ICE))
        {
            return;
        }

        if (level.dimensionType().ultraWarm())
        {
            level.removeBlock(pos, false);
        }
        else
        {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
        }
    }
}