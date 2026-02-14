package net.WWGS.dontfreeze.apiimp.initializer;

import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.mojang.logging.LogUtils;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.api.colony.building.DFBuildings;
import net.WWGS.dontfreeze.api.colony.building.view.GeneratorView;
import net.WWGS.dontfreeze.core.block.DFBlocks;
import net.WWGS.dontfreeze.core.colony.building.workerbuilding.BuildingGenerator;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Dontfreeze.MODID)
public class DFBuildingsInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DFBuildingsInitializer() {
        throw new IllegalStateException("Tried to initialize: DFBuildingsInitializer but this is a Utility class.");
    }

    @SubscribeEvent
    public static void registerBuildings(RegisterEvent event) {
        if (event.getRegistryKey().equals(CommonMinecoloniesAPIImpl.BUILDINGS))
        {
            LOGGER.info("[DF] Registering Buildings.");

            BuildingEntry.Builder generatorBuilder = new BuildingEntry.Builder();
            generatorBuilder.setBuildingBlock(DFBlocks.BLOCK_HUT_GENERATOR.get());
            generatorBuilder.setBuildingProducer(BuildingGenerator::new);
            generatorBuilder.setBuildingViewProducer(() -> GeneratorView::new);
            generatorBuilder.setRegistryName(ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, DFBuildings.GENERATOR_ID));
            generatorBuilder.addBuildingModuleProducer(BuildingModules.MIN_STOCK);
            generatorBuilder.addBuildingModuleProducer(BuildingModules.STATS_MODULE);
            DFBuildings.generator = generatorBuilder.createBuildingEntry();

            registerBuilding(event, DFBuildings.generator);
        }
    }

    protected static void registerBuilding(RegisterEvent event, BuildingEntry buildingEntry) {
        ResourceKey<Registry<BuildingEntry>> buildingsRegistry = CommonMinecoloniesAPIImpl.BUILDINGS;

        if (buildingsRegistry == null) {
            throw new IllegalStateException("[DF] Building registry is null while attempting to register buildings.");
        }

        ResourceLocation registryName = buildingEntry.getRegistryName();

        if (registryName == null) {
            throw new IllegalStateException("[DF] Attempting to register a building with no registry name.");
        }

        event.register(buildingsRegistry, registry -> {
            registry.register(registryName, buildingEntry);
        });
    }
}
