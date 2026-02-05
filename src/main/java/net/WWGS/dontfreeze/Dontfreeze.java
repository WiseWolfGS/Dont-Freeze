package net.WWGS.dontfreeze;

import com.mojang.logging.LogUtils;
import net.WWGS.dontfreeze.api.client.gui.DFMenus;
import net.WWGS.dontfreeze.apiimp.initializer.DFJobsInitializer;
import net.WWGS.dontfreeze.core.block.DFBlocks;
import net.WWGS.dontfreeze.core.item.DFCreativeModeTabs;
import net.WWGS.dontfreeze.core.item.DFItems;
import net.WWGS.dontfreeze.core.network.DFNetworks;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(Dontfreeze.MODID)
public class Dontfreeze {
    public static final String MODID = "dontfreeze";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Dontfreeze(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        DFBlocks.init(modEventBus);
        DFItems.init(modEventBus);
        DFMenus.init(modEventBus);
        DFCreativeModeTabs.init(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, DFConfig.SPEC);
        modEventBus.addListener(DFNetworks::registerPayloads);

        DFJobsInitializer.DEFERRED_REGISTER.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
