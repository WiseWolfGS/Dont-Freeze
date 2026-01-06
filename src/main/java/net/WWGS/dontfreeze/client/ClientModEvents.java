package net.WWGS.dontfreeze.client;

import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.client.gui.ColonyFuelScreen;
import net.WWGS.dontfreeze.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = DontFreeze.MODID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COLONY_FUEL.get(), ColonyFuelScreen::new);
    }
}
