package net.WWGS.dontfreeze.compat.minecolonies.registry;

import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.compat.minecolonies.building.BuildingGenerator;
import net.WWGS.dontfreeze.compat.minecolonies.building.BuildingGeneratorView;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DFMineColoniesBuildings
{
    private DFMineColoniesBuildings() {}

    // 🔑 MineColonies "buildings" 레지스트리 직접 참조
    public static final DeferredRegister<BuildingEntry> BUILDINGS =
            DeferredRegister.create(
                    ResourceKey.createRegistryKey(
                            ResourceLocation.fromNamespaceAndPath("minecolonies", "buildings")
                    ),
                    DontFreeze.MODID
            );

    public static final DeferredHolder<BuildingEntry, BuildingEntry> GENERATOR =
            BUILDINGS.register("generator", () ->
                    new BuildingEntry.Builder()
                            .setBuildingBlock(DontFreeze.HUT_GENERATOR.get())
                            .setBuildingProducer(BuildingGenerator::new)
                            .setBuildingViewProducer(() -> BuildingGeneratorView::new)
                            .setRegistryName(ResourceLocation.fromNamespaceAndPath(DontFreeze.MODID, "generator"))
                            .createBuildingEntry()
            );


    public static void register(IEventBus modEventBus) {
        BUILDINGS.register(modEventBus);
    }
}
