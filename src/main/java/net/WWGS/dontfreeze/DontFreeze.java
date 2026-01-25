package net.WWGS.dontfreeze;

import net.WWGS.dontfreeze.common.config.DFConfig;
import net.WWGS.dontfreeze.common.config.DFConfigValues;
import net.WWGS.dontfreeze.common.network.DFNetwork;
import net.WWGS.dontfreeze.common.registry.*;
import net.WWGS.dontfreeze.integration.Integrations;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(DFConstants.MODID)
public class DontFreeze {
    public DontFreeze(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        DFBlocks.init(modEventBus);
        DFBlockEntities.init(modEventBus);
        DFCreativeTabs.init(modEventBus);
        DFItems.init(modEventBus);
        DFMenus.init(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, DFConfig.SPEC);
        modEventBus.addListener(DFConfigValues::onLoad);

        modEventBus.addListener(DFNetwork::registerPayloads);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(Integrations::init);
    }
}
