package net.WWGS.dontfreeze.core.common.building;

import com.minecolonies.api.blocks.AbstractColonyBlock;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.common.block.DFBlocks;
import net.WWGS.dontfreeze.core.common.building.view.BuildingGeneratorView;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DFBuildings {
    private static final DeferredRegister<BuildingEntry> BUILDINGS =
            DeferredRegister.create(
                    ResourceLocation.fromNamespaceAndPath("minecolonies", "buildings"),
                    Dontfreeze.MODID
            );

    public static final DeferredHolder<BuildingEntry, BuildingEntry> BUILDING_GENERATOR =
            BUILDINGS.register("generator", () ->
                    new BuildingEntry.Builder()
                            .setRegistryName(ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "generator"))
                            .setBuildingBlock((AbstractColonyBlock<?>) DFBlocks.BLOCK_HUT_GENERATOR.get())
                            .setBuildingProducer(BuildingGenerator::new)
                            .setBuildingViewProducer(() -> BuildingGeneratorView::new)
                            .createBuildingEntry()
            );
}
