package net.WWGS.dontfreeze.core.client;

import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.api.client.gui.DFMenus;
import net.WWGS.dontfreeze.core.client.gui.WindowGeneratorCore;
import net.WWGS.dontfreeze.core.client.state.ClientActiveCoreState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Dontfreeze.MODID, value = Dist.CLIENT)
public final class DFClients {
    private static int t = 0;

    private DFClients() {}

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(DFMenus.MENU_GENERATOR_CORE.get(), WindowGeneratorCore::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post e) {
        if (++t % 4 != 0) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        for (BlockPos core : ClientActiveCoreState.getActive()) {
            double x = core.getX() + 0.5;
            double y = core.getY() + 1.05;
            double z = core.getZ() + 0.5;

            // vanilla 파티클로 시작(연기)
            level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
