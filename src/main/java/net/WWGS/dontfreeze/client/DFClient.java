package net.WWGS.dontfreeze.client;

import net.WWGS.dontfreeze.DFConstants;
import net.WWGS.dontfreeze.client.gui.GeneratorCoreScreen;
import net.WWGS.dontfreeze.common.registry.DFMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = DFConstants.MODID, value = Dist.CLIENT)
public final class DFClient {
    private DFClient() {}

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(DFMenus.MENU_GENERATOR_CORE.get(), GeneratorCoreScreen::new);
    }
}