package wwgs.dontfreeze;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.mojang.logging.LogUtils;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import wwgs.dontfreeze.api.util.QueryUtils;
import wwgs.dontfreeze.apiimp.initializer.DFTileEntitiesInitializer;
import wwgs.dontfreeze.core.MineColoniesBuildingQuery;
import wwgs.dontfreeze.core.MineColoniesCitizenQuery;
import wwgs.dontfreeze.core.blocks.DFBlocks;
import wwgs.dontfreeze.core.common.menu.DFMenus;
import wwgs.dontfreeze.core.items.DFCreativeModeTabs;
import wwgs.dontfreeze.core.items.DFItems;
import wwgs.dontfreeze.core.network.DFNetworks;
import wwgs.dontfreeze.core.temperature.heat.modifier.ColonyHeatModifier;
import wwgs.dontfreeze.core.temperature.heat.modifier.WorldHeatSmoothingModifier;

@Mod(Dontfreeze.MODID)
public class Dontfreeze {
    public static final String MODID = "dontfreeze";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation TEMP_SMOOTH_ID =
            ResourceLocation.fromNamespaceAndPath(MODID, "temp_smooth");

    public static final ResourceLocation COLONY_WARMTH_ID =
            ResourceLocation.fromNamespaceAndPath(MODID, "colony_warmth");

    public Dontfreeze(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterTempModifiers);

        DFBlocks.init(modEventBus);
        DFItems.init(modEventBus);
        DFMenus.init(modEventBus);
        DFCreativeModeTabs.init(modEventBus);

        DFTileEntitiesInitializer.BLOCK_ENTITIES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(DFNetworks::registerPayloads);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        QueryUtils.registerColonyQuery(new MineColoniesBuildingQuery());
        QueryUtils.registerCitizenQuery(new MineColoniesCitizenQuery());
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ensureHeatModifiers(player);
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living instanceof AbstractEntityCitizen) {
            ensureHeatModifiers(living);
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

    @SubscribeEvent
    public void onRegisterTempModifiers(TempModifierRegisterEvent event) {
        event.register(COLONY_WARMTH_ID, ColonyHeatModifier::new);
        event.register(TEMP_SMOOTH_ID, WorldHeatSmoothingModifier::new);
    }

    private static void ensureHeatModifiers(LivingEntity entity) {
        if (!Temperature.hasModifier(entity, Temperature.Trait.WORLD, ColonyHeatModifier.class)) {
            Temperature.addModifier(entity, new ColonyHeatModifier(), Temperature.Trait.WORLD, Placement.LAST);
        }
    }
}
