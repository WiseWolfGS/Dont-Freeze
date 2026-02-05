package net.WWGS.dontfreeze.core.colony.entity.ai.workers.crafting;

import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import net.WWGS.dontfreeze.core.colony.building.workerbuilding.BuildingGenerator;
import net.WWGS.dontfreeze.core.colony.job.JobFireKeeper;
import org.jetbrains.annotations.NotNull;

public class EntityAIWorkFireKeeper extends AbstractEntityAICrafting<JobFireKeeper, BuildingGenerator> {
    protected EntityAIWorkFireKeeper(@NotNull JobFireKeeper job) {
        super(job);
    }

    @Override
    public Class<BuildingGenerator> getExpectedBuildingClass() {
        return null;
    }
}
