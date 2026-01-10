package net.WWGS.dontfreeze.domain.fuel.tick;

import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.WWGS.dontfreeze.domain.fuel.storage.ColonyFuelStorage;
import net.WWGS.dontfreeze.network.msg.S2CActiveGeneratorCores;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber
public final class FuelTicker {
    private static final int TICKS_PER_SECOND = 20;

    private FuelTicker() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % TICKS_PER_SECOND != 0) return; // 1초마다

        ServerLevel overworld = server.overworld();
        ColonyFuelStorage storage = ColonyFuelStorage.get(overworld);

        int consumePerSecond = 20; // TODO: Config로 이동
        if (consumePerSecond <= 0) return;

        // 1) 연료 소비
        for (int colonyId : storage.getColoniesWithFuel()) {
            storage.consumeFuel(colonyId, consumePerSecond);
        }

        // 2) 활성 코어 좌표 수집 (연료 > 0 인 콜로니만)
        Set<BlockPos> activeCores = new HashSet<>();
        for (int colonyId : storage.getColoniesWithFuel()) {
            if (storage.getFuel(colonyId) <= 0) continue;

            for (BlockPos hutPos : MineColoniesCompat.getGeneratorHutPositions(overworld, colonyId)) {
                activeCores.add(hutPos.above(1));
            }
        }

        // 3) 전송 (범위 최적화는 나중에)
        S2CActiveGeneratorCores msg = new S2CActiveGeneratorCores(activeCores);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(sp, msg);
        }
    }
}
