package wwgs.dontfreeze.core;

import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.api.colony.citizen.query.CitizenQuery;

public class MineColoniesCitizenQuery implements CitizenQuery {
    @Override
    public void migrateCitizen(IColony newColony, ICivilianData oldData) {
        if (newColony == null || oldData == null) return;

        IColony oldColony = oldData.getColony();
        if (oldColony == null || oldColony == newColony) return;

        var oldMgr = oldColony.getCitizenManager();
        var newMgr = newColony.getCitizenManager();

        var opt = oldData.getEntity();
        if (opt.isEmpty()) return;

        var entity = opt.get();
        entity.getNavigation().stop();

        oldMgr.unregisterCivilian(entity);
        oldData.setEntity(null);
        oldData.markDirty(0);

        ICivilianData newData = newMgr.createAndRegisterCivilianData();
        newData.initForNewCivilian();

        ServerLevel level = (ServerLevel) newColony.getWorld();
        var provider = level.registryAccess();

        var tag = oldData.serializeNBT(provider);
        newData.deserializeNBT(provider, tag);

        newData.setEntity(entity);
        newData.initEntityValues();
        newData.updateEntityIfNecessary();
        newData.markDirty(0);

        newMgr.registerCivilian(entity);
        newMgr.markDirty();

        oldMgr.removeCivilian(oldData);
        oldMgr.markDirty();

        level = (ServerLevel) newColony.getWorld();
        BlockPos target = newColony.getCenter();

        newData.updateEntityIfNecessary();

        level.getServer().execute(() -> {
            newData.updateEntityIfNecessary();

            newData.getEntity().ifPresent(e -> {
                e.teleportTo(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
                e.getNavigation().stop();
                e.setDeltaMovement(0, 0, 0);
                e.hasImpulse = true;

                Dontfreeze.LOGGER.info("final tp entity uuid={} pos={} removed={}",
                        e.getUUID(), e.blockPosition(), e.isRemoved());
            });
        });
    }
}
