package net.WWGS.dontfreeze.client.tick;

import net.WWGS.dontfreeze.client.state.ClientHeaterState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class HeatTicker {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;

        // 파티클 과다 방지
        if (level.getGameTime() % 4 != 0) return;

        for (BlockPos pos : ClientHeaterState.ACTIVE_GENERATOR_CORES) {
            // 거리 제한
            if (mc.player.distanceToSqr(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
            ) > 64 * 64) continue;

            spawnSmoke(level, pos);
        }
    }

    private static void spawnSmoke(Level level, BlockPos pos) {
        double x = pos.getX() + 0.3 + level.random.nextDouble() * 0.4;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.3 + level.random.nextDouble() * 0.4;

        level.addParticle(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                x, y, z,
                0.0, 0.02, 0.0
        );
    }
}

