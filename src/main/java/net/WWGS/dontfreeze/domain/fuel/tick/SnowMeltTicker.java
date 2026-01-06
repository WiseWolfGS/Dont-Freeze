package net.WWGS.dontfreeze.domain.fuel.tick;

import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.WWGS.dontfreeze.domain.fuel.storage.ColonyFuelStorage;
import net.WWGS.dontfreeze.domain.heat.storage.ColonyHeatStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import static net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat.meltSnowAndIceInChunk;

@EventBusSubscriber
public final class SnowMeltTicker {
    private SnowMeltTicker() {}

    private static final int INTERVAL_TICKS = 20 * 10;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL_TICKS != 0) return; // 10초마다

        ServerLevel level = server.overworld();
        ColonyFuelStorage fuel = ColonyFuelStorage.get(level);
        ColonyHeatStorage heat = ColonyHeatStorage.get(level);

        for (int colonyId : fuel.getColoniesWithFuel()) {
            if (fuel.getFuel(colonyId) <= 0) continue;

            BlockPos center = heat.getTownHallPos(colonyId);
            if (center == null) continue;

            for (ChunkPos cp : MineColoniesCompat.getClaimedChunksForColony(level, colonyId)) {
                meltSnowAndIceInChunk(level, cp);
            }
        }

    }
}
