package net.WWGS.dontfreeze.core.colony.building.module;

import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import net.WWGS.dontfreeze.api.colony.building.job.DFJobs;

public class DFBuildingModules {
    public static final BuildingEntry.ModuleProducer<WorkerBuildingModule, WorkerBuildingModuleView> FIREKEEPER_WORK =
            new BuildingEntry.ModuleProducer<>(
                    "firekeeper_work",
                    () -> new WorkerBuildingModule(DFJobs.firekeeper.get(), Skill.Stamina, Skill.Focus, false, (b) -> 1),
                    () -> WorkerBuildingModuleView::new
            );
}
