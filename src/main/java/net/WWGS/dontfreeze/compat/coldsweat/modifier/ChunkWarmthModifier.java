package net.WWGS.dontfreeze.compat.coldsweat.modifier;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.WWGS.dontfreeze.domain.fuel.storage.ColonyFuelStorage;
import net.WWGS.dontfreeze.domain.heat.model.ChunkHeatRef;
import net.WWGS.dontfreeze.domain.heat.service.ChunkHeatCache;
import net.WWGS.dontfreeze.domain.heat.storage.ColonyHeatStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public class ChunkWarmthModifier extends TempModifier {

    @Override
    protected Function<Double, Double> calculate(LivingEntity livingEntity, Temperature.Trait trait) {
        if (!(livingEntity.level() instanceof ServerLevel level)) {
            return Function.identity();
        }

        // 오버월드가 아니면 난방 적용 안 함
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return Function.identity();
        }

        ChunkPos cp = livingEntity.chunkPosition();

        // 청크 캐시에서 참조 얻기 (이 청크가 "어떤 콜로니와 연관되는지" 계산)
        ChunkHeatRef ref = ChunkHeatCache.get(level).getOrCompute(level, cp);
        if (!ref.isValid()) return Function.identity();

        int colonyId = ref.colonyId();

        // MineColonies 실제 클레임(영역)과 정확히 일치시키기
        if (!MineColoniesCompat.isChunkClaimedByColony(level, cp, colonyId)) {
            return Function.identity();
        }

        int fuel = ColonyFuelStorage.get(level).getFuel(colonyId);
        if (fuel <= 0) return Function.identity();

        var params = ColonyHeatStorage.get(level).getParams(colonyId);
        double bonus = Math.max(0.0, params.bonus());

        // Cold Sweat 쪽 온도 보정 상한
        double maxTemp = 1.0;

        if (bonus <= 0.0) return Function.identity();

        return temp -> {
            if (temp >= maxTemp) return temp;

            if (temp > 0) {
                double allowed = maxTemp - temp;
                double applied = Math.min(bonus, allowed);
                return temp + applied;
            }

            return Math.min(temp + bonus, maxTemp);
        };
    }
}
