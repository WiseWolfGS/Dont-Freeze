package net.WWGS.dontfreeze;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.items.ItemBlockHut;
import com.mojang.logging.LogUtils;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import net.WWGS.dontfreeze.compat.coldsweat.modifier.ChunkWarmthModifier;
import net.WWGS.dontfreeze.compat.minecolonies.block.BlockHutGenerator;
import net.WWGS.dontfreeze.compat.minecolonies.registry.DFMineColoniesBuildings;
import net.WWGS.dontfreeze.registry.ModMenus;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

// 여기서 사용하는 값들은 META-INF/neoforge.mods.toml의 설정과 일치하게 맞춰야 함.
@Mod(DontFreeze.MODID)
public class DontFreeze {
    public static final String MODID = "dontfreeze"; // 모드 전역에서 참조할 수 있도록 MODID를 공용 상수로 정의.
    public static final Logger LOGGER = LogUtils.getLogger(); // 로그 출력용 SLF4J Logger.

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID); // DeferredRegister 생성: "dontfreeze" 네임스페이스로 등록될 블록들을 보관(등록 대기).
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID); // DeferredRegister 생성: "dontfreeze" 네임스페이스로 등록될 아이템들을 보관(등록 대기).
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID); // DeferredRegister 생성: "dontfreeze" 네임스페이스로 등록될 크리에이티브 탭들을 보관(등록 대기).
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE)); // "dontfreeze:example_block" ID로 새 블록을 등록. (네임스페이스 + 경로)
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK); // 위 블록에 대응하는 BlockItem을 "dontfreeze:example_block" ID로 등록.

    public static final DeferredBlock<Block> GENERATOR_CORE_BLOCK =
            BLOCKS.registerSimpleBlock("generator_core",
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0f, 6.0f)     // 단단함/폭발저항
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> 12)  // 발광(0~15)
            );

    public static final DeferredBlock<AbstractBlockHut<?>> HUT_GENERATOR =
            BLOCKS.register("blockhutgenerator",
                    () -> new BlockHutGenerator(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                    )
            );

    public static final DeferredItem<Item> HUT_GENERATOR_ITEM =
            ITEMS.register("blockhutgenerator",
                    () -> new ItemBlockHut(HUT_GENERATOR.get(), new Item.Properties())
            );



    public static final DeferredItem<BlockItem> GENERATOR_CORE_ITEM =
            ITEMS.registerSimpleBlockItem("generator_core", GENERATOR_CORE_BLOCK);


    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build())); // 영양도 1, 포만도 2를 가진 음식 아이템("dontfreeze:example_item")을 등록.

    // "dontfreeze:example_tab" ID의 크리에이티브 탭을 생성하며, 전투 탭 뒤에 배치.
    @SuppressWarnings("unused")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register(
            "example_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.dontfreeze"))
                            .withTabsBefore(CreativeModeTabs.COMBAT).icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(EXAMPLE_ITEM.get());
                                output.accept(EXAMPLE_BLOCK_ITEM.get());
                                output.accept(GENERATOR_CORE_ITEM.get());
                                output.accept(HUT_GENERATOR_ITEM.get());
                                // 예제 아이템을 이 탭에 추가하는 코드.
                                // 커스텀 탭을 만들 때는 이벤트보다 이 방식이 권장됨.
                            }).build());

    // 아래 있는 모드 클래스의 생성자는 모드가 로드될 때 가장 먼저 실행되는 코드.
    public DontFreeze(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup); // 모드 로딩 단계에서 실행될 commonSetup 메서드를 등록.

        BLOCKS.register(modEventBus); // 블록이 실제로 등록되도록 DeferredRegister를 모드 이벤트 버스에 연결.
        ITEMS.register(modEventBus); // 아이템 등록을 위해 DeferredRegister를 모드 이벤트 버스에 연결.
        CREATIVE_MODE_TABS.register(modEventBus); // 크리에이티브 탭 등록을 위해 DeferredRegister를 모드 이벤트 버스에 연결.

        NeoForge.EVENT_BUS.register(this); // 서버 시작 등, 이 클래스에서 직접 처리할 게임 이벤트들을 수신하도록 등록. 이 코드는 DontFreeze 클래스 자체가 이벤트에 반응해야 할 경우에만 필요하니, 아래의 onServerStarting()처럼 @SubscribeEvent가 붙은 메서드가 없다면 등록하지 않아도 됨.

        modEventBus.addListener(this::addCreative); // 아이템을 기존 크리에이티브 탭에 추가하기 위한 이벤트를 등록.
        ModMenus.MENUS.register(modEventBus);
        DFMineColoniesBuildings.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC); // // FML이 우리 모드의 설정 파일을 생성하고 로드할 수 있도록 ModConfigSpec을 등록.
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(BlueprintPackInstaller::ensureInstalled);
    }

    // 예제 블록 아이템을 '건축 블록' 탭에 추가.
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
            event.accept(GENERATOR_CORE_ITEM);
        }
    }

    // @SubscribeEvent를 사용하면 이벤트 버스가 자동으로 호출할 메서드를 찾아 실행함.
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!Temperature.hasModifier(player, Temperature.Trait.WORLD, ChunkWarmthModifier.class)) {
            Temperature.addModifier(player, new ChunkWarmthModifier(), Temperature.Trait.WORLD, Placement.LAST);
        }
    }

    // EventBusSubscriber를 사용하면 이 클래스의 static 메서드 중 @SubscribeEvent가 붙은 것들을 자동으로 이벤트 버스에 등록할 수 있음.
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ServerModEvents {
        @SubscribeEvent
        public static void onServerSetup(FMLDedicatedServerSetupEvent event) {
        }
    }
}
