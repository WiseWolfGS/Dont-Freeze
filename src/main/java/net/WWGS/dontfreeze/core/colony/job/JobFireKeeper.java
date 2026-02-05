package net.WWGS.dontfreeze.core.colony.job;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import net.WWGS.dontfreeze.core.colony.entity.ai.workers.crafting.EntityAIWorkFireKeeper;

public class JobFireKeeper extends AbstractJobCrafter<EntityAIWorkFireKeeper, JobFireKeeper> {

    public JobFireKeeper(ICitizenData entity) {
        super(entity);
    }

    @Override
    public EntityAIWorkFireKeeper generateAI() {
        return null;
    }

    @Override
    public void onLevelUp() {
        super.onLevelUp();
    }

    @Override
    public void initEntityValues(AbstractEntityCitizen citizen) {
        super.initEntityValues(citizen);
    }

    @Override
    public int getInactivityLimit() {
        return super.getInactivityLimit();
    }

    @Override
    public int getIdleSeverity(boolean isDemand) {
        return super.getIdleSeverity(isDemand);
    }

    @Override
    public void triggerActivityChangeAction(boolean newState) {
        super.triggerActivityChangeAction(newState);
    }

    @Override
    public boolean isGuard() {
        return super.isGuard();
    }

    @Override
    public double getSaturationFactor() {
        return super.getSaturationFactor();
    }
}
