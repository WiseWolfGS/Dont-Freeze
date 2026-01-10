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
import net.minecraft.world.level.block.entity.BlockEntity;

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
}
