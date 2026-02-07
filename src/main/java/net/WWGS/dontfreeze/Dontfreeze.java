package net.WWGS.dontfreeze;

import com.minecolonies.apiimp.initializer.ModBlocksInitializer;
import com.mojang.logging.LogUtils;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import net.WWGS.dontfreeze.api.client.gui.DFMenus;
import net.WWGS.dontfreeze.api.util.QueryUtils;
import net.WWGS.dontfreeze.apiimp.initializer.DFJobsInitializer;
import net.WWGS.dontfreeze.apiimp.initializer.DFTileEntitiesInitializer;
import net.WWGS.dontfreeze.core.block.DFBlocks;
import net.WWGS.dontfreeze.core.colony.MineColoniesColonyQuery;
import net.WWGS.dontfreeze.core.colony.MineColoniesGeneratorQuery;
import net.WWGS.dontfreeze.core.colony.heat.platform.ColonyHeatModifier;
import net.WWGS.dontfreeze.core.colony.heat.platform.WorldHeatSmoothingModifier;
import net.WWGS.dontfreeze.core.item.DFCreativeModeTabs;
import net.WWGS.dontfreeze.core.item.DFItems;
import net.WWGS.dontfreeze.core.network.DFNetworks;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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

        DFTileEntitiesInitializer.BLOCK_ENTITIES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, DFConfig.SPEC);
        modEventBus.addListener(DFNetworks::registerPayloads);

        DFJobsInitializer.DEFERRED_REGISTER.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        QueryUtils.registerColonyQuery(new MineColoniesColonyQuery());
        QueryUtils.registerGeneratorQuery(new MineColoniesGeneratorQuery());
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!Temperature.hasModifier(player, Temperature.Trait.WORLD, ColonyHeatModifier.class)) {
            Temperature.addModifier(player, new ColonyHeatModifier(), Temperature.Trait.WORLD, Placement.LAST);
        }

        if (!Temperature.hasModifier(player, Temperature.Trait.CORE, WorldHeatSmoothingModifier.class)) {
            player.getPersistentData().remove("dontfreeze:temp_smoothing_last_core");
            Temperature.addModifier(player, new WorldHeatSmoothingModifier(), Temperature.Trait.CORE, Placement.LAST);
        }
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
