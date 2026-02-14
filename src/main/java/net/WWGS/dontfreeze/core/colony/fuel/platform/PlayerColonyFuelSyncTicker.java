package net.WWGS.dontfreeze.core.colony.fuel.platform;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.claim.IChunkClaimData;
import com.minecolonies.api.colony.permissions.IPermissions;
import net.WWGS.dontfreeze.DFConfig;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.colony.heat.storage.ColonyHeatParamsStorage;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelCostCalculator;
import net.WWGS.dontfreeze.core.colony.building.workerbuilding.BuildingGenerator;
import net.WWGS.dontfreeze.core.network.payload.S2CPlayerColonyFuelTime;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * HUD에서 사용할 수 있도록, 서버가 각 플레이어에게 "소속 콜로니"의 발전기 남은 시간을 주기적으로 전송.
 *
 * 소속 콜로니 판정은 (요청하신 3번 방식) "권한 시스템에 플레이어가 등록되어 있는지"를 기준으로 스캔합니다.
 * 플레이어가 여러 콜로니에 등록되어 있다면, 현재 플레이어 위치에서 가장 가까운 TownHall 을 가진 콜로니를 선택합니다.
 */
@EventBusSubscriber(modid = Dontfreeze.MODID)
public final class PlayerColonyFuelSyncTicker {
    private PlayerColonyFuelSyncTicker() {}

    private static final int PERIOD_TICKS = 20; // 1초
    private static final int SECONDS_PER_MINUTE = 60;
    private static int ticker = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (++ticker % PERIOD_TICKS != 0) return;

        MinecraftServer server = e.getServer();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (!(sp.level() instanceof ServerLevel level)) continue;

            int colonyId = findMembershipColonyIdClosestTownHall(level, sp);
            int minutes = 0;
            int seconds = 0;
            double tickPerFuel = 0;

            if (colonyId > 0) {
                int fuel = FuelSavedData.get(level).getFuel(colonyId);

                // ✅ Include fuel items stored in the Generator building's own inventory.
                // This is limited to the Generator building(s) in the colony.
                fuel += getGeneratorBuildingInventoryFuelTicks(level, colonyId);
                if (fuel > 0) {
                    double bonus = ColonyHeatParamsStorage.get(level).getParams(colonyId).bonus();
                    int costPerSecond = FuelCostCalculator.compute(level, colonyId, bonus).totalCostPerSecond();
                    if (costPerSecond > 0) {
                        int totalSeconds = fuel / costPerSecond;
                        minutes = totalSeconds / SECONDS_PER_MINUTE;
                        seconds = totalSeconds % SECONDS_PER_MINUTE;
                        tickPerFuel = costPerSecond;
                    }
                }
            }

            PacketDistributor.sendToPlayer(sp, new S2CPlayerColonyFuelTime(colonyId, minutes, seconds, tickPerFuel));
        }
    }

    /**
     * Sum of burn ticks for all fuel items in Generator building inventory(ies) within the colony.
     */
    private static int getGeneratorBuildingInventoryFuelTicks(ServerLevel level, int colonyId) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return 0;

        IColony colony;
        try {
            colony = manager.getColonyByWorld(colonyId, level);
        } catch (Throwable t) {
            return 0;
        }
        if (colony == null) return 0;

        Map<BlockPos, IBuilding> buildings;
        try {
            buildings = colony.getServerBuildingManager().getBuildings();
        } catch (Throwable t) {
            return 0;
        }
        if (buildings == null || buildings.isEmpty()) return 0;

        long total = 0;

        for (IBuilding b : buildings.values()) {
            if (b == null) continue;

            // Fast-path: our building class
            if (b instanceof BuildingGenerator gen) {
                total += gen.getTotalFuelTicksIncludingHandlers();
                continue;
            }

            // Compat: match by schematic name and try reflectively
            boolean isGenerator = false;
            try {
                isGenerator = "generator".equals(b.getSchematicName());
            } catch (Throwable ignored) {}
            if (!isGenerator) continue;

            try {
                var m = b.getClass().getMethod("getTotalFuelTicksIncludingHandlers");
                m.setAccessible(true);
                Object v = m.invoke(b);
                if (v instanceof Integer i) total += Math.max(0, i);
            } catch (Throwable ignored) {
            }
        }

        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /**
     * 3번 방식: 권한 시스템에 "등록된" 콜로니를 찾는다.
     *
     * - MineColonies가 claim 기반으로 콜로니를 보관하므로, claim map을 스캔하여 colonyId 후보를 얻고
     * - 각 colony의 permissions에서 UUID가 NEUTRAL이 아닌지(=등록되어 있는지) 확인한다.
     * - 여러 개면 TownHall이 플레이어 위치와 가장 가까운 colony를 선택한다.
     */
    private static int findMembershipColonyIdClosestTownHall(ServerLevel level, ServerPlayer player) {
        IColonyManager manager = IColonyManager.getInstance();
        if (manager == null) return -1;

        Set<Integer> colonyIds = collectColonyIds(manager, level);
        if (colonyIds.isEmpty()) return -1;

        UUID uuid = player.getUUID();
        BlockPos ppos = player.blockPosition();

        double bestDist2 = Double.MAX_VALUE;
        int bestId = -1;

        for (int id : colonyIds) {
            if (id <= 0) continue;
            IColony colony = null;
            try {
                colony = manager.getColonyByWorld(id, level);
            } catch (Throwable ignored) {}
            if (colony == null) continue;

            IPermissions perms;
            try {
                perms = colony.getPermissions();
            } catch (Throwable t) {
                continue;
            }
            if (perms == null) continue;

            if (!isRegisteredMember(perms, uuid)) continue;

            BlockPos thPos = townHallPos(colony);
            if (thPos == null) continue;

            double d2 = thPos.distSqr(ppos);
            if (d2 < bestDist2) {
                bestDist2 = d2;
                bestId = id;
            }
        }

        return bestId;
    }

    private static Set<Integer> collectColonyIds(IColonyManager manager, ServerLevel level) {
        try {
            ResourceKey<Level> dim = level.dimension();
            Map<ChunkPos, IChunkClaimData> claimMap = manager.getClaimData(dim);
            if (claimMap == null || claimMap.isEmpty()) return Set.of();

            Set<Integer> ids = new HashSet<>();
            for (IChunkClaimData data : claimMap.values()) {
                if (data == null) continue;
                int id = data.getOwningColony();
                if (id > 0) ids.add(id);
            }
            return ids;
        } catch (Throwable ignored) {
            return Set.of();
        }
    }

    private static BlockPos townHallPos(IColony colony) {
        try {
            ITownHall th = colony.getServerBuildingManager().getTownHall();
            return (th == null) ? null : th.getPosition();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * MineColonies 버전별로 permissions API가 조금씩 달라서, reflection으로 "등록 여부"를 최대한 안전하게 판정.
     *
     * 목표: NEUTRAL(기본/방문자)만 아니면 "소속(멤버/친구/오피서/오너 등)"으로 간주.
     */
    private static boolean isRegisteredMember(IPermissions perms, UUID uuid) {
        // 1) getRank(UUID) / getUserRank(UUID) 류가 있으면 우선 사용
        Object rank = invoke(perms, "getRank", uuid);
        if (rank == null) rank = invoke(perms, "getUserRank", uuid);
        if (rank == null) rank = invoke(perms, "getPermissionLevel", uuid);

        if (rank != null) {
            // enum이면 name() 비교
            if (rank instanceof Enum<?> e) {
                return !"NEUTRAL".equalsIgnoreCase(e.name());
            }
            // 문자열이면 그대로 비교
            if (rank instanceof String s) {
                return !"NEUTRAL".equalsIgnoreCase(s);
            }
        }

        // 2) 직접 member 체크 메서드가 있으면 사용
        Object b = invoke(perms, "isColonyMember", uuid);
        if (b instanceof Boolean bb) return bb;
        b = invoke(perms, "isPlayerInColony", uuid);
        if (b instanceof Boolean bb2) return bb2;

        // 3) 마지막 수단: 플레이어를 key로 하는 맵/셋을 반환하는 메서드가 있으면 contains 검사
        Object users = invoke(perms, "getPlayers");
        if (users instanceof java.util.Map<?, ?> m) {
            return m.containsKey(uuid);
        }
        if (users instanceof java.util.Set<?> s) {
            return s.contains(uuid);
        }

        return false;
    }

    private static Object invoke(Object target, String method, Object... args) {
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = args[i].getClass();
            }
            var m = target.getClass().getMethod(method, types);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
