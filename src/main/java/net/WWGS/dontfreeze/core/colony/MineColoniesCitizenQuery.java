package net.WWGS.dontfreeze.core.colony;

import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.citizen.AbstractCivilianEntity;
import com.mojang.logging.LogUtils;
import net.WWGS.dontfreeze.api.colony.CitizenQuery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;

import java.util.Objects;

import static net.WWGS.dontfreeze.api.util.QueryUtils.colonyQuery;


public class MineColoniesCitizenQuery implements CitizenQuery {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Integer getColonyIdByCitizen(ICivilianData civilianData) {
        IColony colony = civilianData.getColony();
        return colony == null ? null : colony.getID();
    }

    @Override
    public void migrateCitizen(IColony newColony, ICivilianData oldData) {
        if (newColony == null || oldData == null) return;

        IColony oldColony = oldData.getColony();
        if (oldColony == null || oldColony == newColony) return;

        var oldMgr = oldColony.getCitizenManager();
        var newMgr = newColony.getCitizenManager();

        // 0) 엔티티 확보 (remove 전에!)
        var opt = oldData.getEntity();
        if (opt.isEmpty()) return;

        var entity = opt.get(); // AbstractCivilianEntity

        // 1) 네비 정지
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.getNavigation().stop();
        }

        // 2) old 쪽에서 엔티티 등록 해제 + 데이터와 분리
        oldMgr.unregisterCivilian(entity);
        oldData.setEntity(null);
        oldData.markDirty(0);

        // 3) 새 데이터 생성
        ICivilianData newData = newMgr.createAndRegisterCivilianData();
        newData.initForNewCivilian();

        // 4) NBT 복사 (★ Provider 필수)
        ServerLevel level = (ServerLevel) newColony.getWorld();
        var provider = level.registryAccess();

        var tag = oldData.serializeNBT(provider);
        newData.deserializeNBT(provider, tag);

        // 5) 새 데이터에 기존 엔티티 연결
        newData.setEntity(entity);
        newData.initEntityValues();
        newData.updateEntityIfNecessary();
        newData.markDirty(0);

        // 6) newColony에 엔티티 등록
        newMgr.registerCivilian(entity);
        newMgr.markDirty();

        // 7) oldColony에서 데이터 제거 (엔티티는 이미 분리됨)
        oldMgr.removeCivilian(oldData);
        oldMgr.markDirty();

        level = (ServerLevel) newColony.getWorld();
        BlockPos target = newColony.getCenter();

        newData.updateEntityIfNecessary();

        level.getServer().execute(() -> {
            newData.updateEntityIfNecessary();

            newData.getEntity().ifPresent(e -> {
                e.teleportTo(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);

                // 보험: 네비/속도 초기화
                if (e instanceof net.minecraft.world.entity.Mob mob) {
                    mob.getNavigation().stop();
                    mob.setDeltaMovement(0, 0, 0);
                    mob.hasImpulse = true;
                }

                LOGGER.info("final tp entity uuid={} pos={} removed={}",
                        e.getUUID(), e.blockPosition(), e.isRemoved());
            });
        });
    }
}
