package net.WWGS.dontfreeze.compat.minecolonies;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.claim.IChunkClaimData;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.compat.minecolonies.building.BuildingGenerator;
import net.WWGS.dontfreeze.compat.minecolonies.registry.DFMineColoniesBuildings;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * MineColonies 관련 조회/유틸 모음.
 * <p>
 * NOTE: MineColonies 버전 차이로 인해 일부 API는 리플렉션/폴백 로직을 포함한다.
 */
public final class MineColoniesCompat {
    private MineColoniesCompat() {}

    public static void init() {
        DontFreeze.LOGGER.info("MineColoniesCompat initialized");
    }

    /** 주어진 위치에서 가장 가까운 콜로니의 타운홀 위치를 반환 */
    public static @Nullable BlockPos findNearestTownHall(ServerLevel level, BlockPos pos) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return null;

        IColony colony = manager.getClosestIColony(level, pos);
        if (colony == null) return null;

        ITownHall townHall = colony.getBuildingManager().getTownHall();
        if (townHall == null) return null;

        return townHall.getPosition();
    }

    /** 타운홀 블록 엔티티에서 colonyId 추출 */
    public static @Nullable Integer getColonyIdFromTownHall(ServerLevel level, BlockPos townHallPos) {
        BlockEntity be = level.getBlockEntity(townHallPos);
        if (!(be instanceof TileEntityColonyBuilding colonyBE)) return null;

        IBuilding building = colonyBE.getBuilding();
        if (building == null) return null;

        return building.getColony().getID();
    }

    /** colonyId로 타운홀 위치 조회 */
    @Nullable
    public static BlockPos getTownHallPos(ServerLevel level, int colonyId) {
        IColony colony = IColonyManager.getInstance().getColonyByWorld(colonyId, level);
        if (colony == null) return null;

        ITownHall townHall = colony.getBuildingManager().getTownHall();
        if (townHall == null) return null;

        return townHall.getPosition();
    }

    /**
     * colonyId가 보유한 'generator' 건물들의 hut 위치를 반환.
     * <p>
     * MineColonies 버전 차이로 building type getter가 다를 수 있어, 3단계 폴백을 둔다:
     * <ol>
     *   <li>BuildingEntry 비교 (가능하면 가장 정확)</li>
     *   <li>우리 BuildingGenerator 인스턴스 여부</li>
     *   <li>schematicName == "generator"</li>
     * </ol>
     */
    public static Set<BlockPos> getGeneratorHutPositions(ServerLevel level, int colonyId) {
        IColony colony = IColonyManager.getInstance().getColonyByWorld(colonyId, level);
        if (colony == null) return Collections.emptySet();

        Map<BlockPos, IBuilding> buildings = colony.getBuildingManager().getBuildings();
        if (buildings.isEmpty()) return Collections.emptySet();

        var generatorEntry = DFMineColoniesBuildings.GENERATOR.get();
        Set<BlockPos> out = new HashSet<>();

        for (IBuilding b : buildings.values()) {
            if (b == null) continue;

            // 1) BuildingEntry 비교
            try {
                Object type = b.getClass().getMethod("getBuildingType").invoke(b);
                if (type == generatorEntry) {
                    out.add(b.getPosition());
                    continue;
                }
            } catch (Throwable ignored) {
            }

            // 2) 인스턴스 비교
            if (b instanceof BuildingGenerator) {
                out.add(b.getPosition());
                continue;
            }

            // 3) 스키매틱 이름 비교
            try {
                if ("generator".equals(b.getSchematicName())) {
                    out.add(b.getPosition());
                }
            } catch (Throwable ignored) {
            }
        }

        return out;
    }

    /** colonyId가 소유한 청크 클레임 목록 */
    public static Set<ChunkPos> getClaimedChunksForColony(ServerLevel level, int colonyId) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return Set.of();

        ResourceKey<Level> dim = level.dimension();
        Map<ChunkPos, IChunkClaimData> map = manager.getClaimData(dim);
        if (map == null || map.isEmpty()) return Set.of();

        Set<ChunkPos> out = new HashSet<>();
        for (var e : map.entrySet()) {
            IChunkClaimData data = e.getValue();
            if (data != null && data.getOwningColony() == colonyId) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /** 특정 청크가 colonyId에 의해 클레임되었는지 */
    public static boolean isChunkClaimedByColony(ServerLevel level, ChunkPos chunkPos, int colonyId) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return false;

        IChunkClaimData data = manager.getClaimData(level.dimension(), chunkPos);
        return data != null && data.getOwningColony() == colonyId;
    }

    /** 청크 내 눈/얼음 제거(간단 버전) */
    public static void meltSnowAndIceInChunk(ServerLevel level, ChunkPos cp) {
        if (!level.hasChunk(cp.x, cp.z)) return;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = cp.getMinBlockX() + dx;
                int z = cp.getMinBlockZ() + dz;

                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                if (y < level.getMinBuildHeight()) continue;

                BlockPos base = new BlockPos(x, y, z);

                for (int up = 0; up <= 1; up++) {
                    BlockPos p = base.above(up);
                    BlockState s = level.getBlockState(p);

                    if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK)) {
                        level.removeBlock(p, false);
                    } else if (s.is(Blocks.ICE)) {
                        level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
                    }
                }
            }
        }
    }
}
