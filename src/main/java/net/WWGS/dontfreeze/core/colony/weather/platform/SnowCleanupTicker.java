package net.WWGS.dontfreeze.core.colony.weather.platform;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.claim.IChunkClaimData;
import com.mojang.logging.LogUtils;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.api.colony.ColonyQuery;
import net.WWGS.dontfreeze.api.util.QueryUtils;
import net.WWGS.dontfreeze.core.colony.fuel.platform.GeneratorCoreRegistry;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelService;
import net.WWGS.dontfreeze.core.colony.heat.storage.ColonyHeatParamsStorage;
import net.WWGS.dontfreeze.core.colony.weather.srm.SrmCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Dontfreeze.MODID)
public final class SnowCleanupTicker {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 40틱(2초)마다 "로드된 클레임 청크" 표면만 훑어서 정리
    private static final int PERIOD_TICKS = 40;
    private static int ticker = 0;

    private SnowCleanupTicker() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (++ticker % PERIOD_TICKS != 0) return;

        ServerLevel level = e.getServer().overworld();

        ColonyQuery cq = QueryUtils.colonyQuery();
        if (cq == null) return;

        Set<Integer> heatedColonies = collectHeatedColonyIds(level, cq);
        if (heatedColonies.isEmpty()) return;

        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return;

        Map<ChunkPos, IChunkClaimData> claimMap = manager.getClaimData(level.dimension());
        if (claimMap == null || claimMap.isEmpty()) return;

        for (Map.Entry<ChunkPos, IChunkClaimData> en : claimMap.entrySet()) {
            IChunkClaimData data = en.getValue();
            if (data == null) continue;

            int colonyId = data.getOwningColony();
            if (!heatedColonies.contains(colonyId)) continue;

            ChunkPos cp = en.getKey();
            if (cp == null) continue;

            // 청크를 강제로 로드하지 않기: 로드된 경우만 처리
            if (!level.hasChunk(cp.x, cp.z)) continue;

            cleanupChunkSurface(level, cp);
        }
    }

    private static Set<Integer> collectHeatedColonyIds(ServerLevel level, ColonyQuery cq) {
        Set<Integer> ids = new HashSet<>();
        for (BlockPos pos : GeneratorCoreRegistry.get(level).getAll()) {
            Integer colonyId = cq.findColonyIdAtPos(level, pos);
            if (colonyId == null || colonyId <= 0) continue;

            if (FuelService.getFuel(level, colonyId) <= 0) continue;

            double bonus = ColonyHeatParamsStorage.get(level).getParams(colonyId).bonus();
            if (bonus <= 0.0) continue;

            ids.add(colonyId);
        }
        return ids;
    }

    private static void cleanupChunkSurface(ServerLevel level, ChunkPos cp) {
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                int yTopAir = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                // 표면 근처만(위 1칸/아래 2칸) 검사
                for (int y = yTopAir - 2; y <= yTopAir + 1; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.isLoaded(p)) continue;

                    BlockState st = level.getBlockState(p);

                    if (st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.POWDER_SNOW)) {
                        level.removeBlock(p, false);
                        continue;
                    }

                    if (SrmCompat.isSrmBlock(level, st)) {
                        if (!SrmCompat.tryRestoreOriginal(level, p)) {
                            LOGGER.debug("[DontFreeze] SRM restore failed at {} state={}", p, st);
                        }
                    }
                }
            }
        }
    }
}
