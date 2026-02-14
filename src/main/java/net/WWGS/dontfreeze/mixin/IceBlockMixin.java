package net.WWGS.dontfreeze.mixin;

import net.WWGS.dontfreeze.api.colony.ColonyQuery;
import net.WWGS.dontfreeze.api.util.QueryUtils;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelService;
import net.WWGS.dontfreeze.core.colony.heat.storage.ColonyHeatParamsStorage;
import net.WWGS.dontfreeze.core.colony.weather.HeatInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void dontfreeze$meltIceInHeatedArea(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource rand,
            CallbackInfo ci
    ) {
        HeatInfo hi = getHeatInfo(level, pos);
        if (hi == null) return;

        // 난방 중이면 우리가 확률로 녹이고(물로 변환) 바닐라 로직은 막아버림
        ci.cancel();

        // 확률: 최소 10% ~ 최대 80%
        double chance = Math.min(0.80, 0.10 + hi.bonus() * 0.10);
        if (rand.nextDouble() > chance) return;

        if (level.dimensionType().ultraWarm()) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        }
    }

    private static HeatInfo getHeatInfo(ServerLevel level, BlockPos pos) {
        ColonyQuery cq = QueryUtils.colonyQuery();
        if (cq == null) return null;

        Integer colonyId = cq.findColonyIdAtPos(level, pos);
        if (colonyId == null || colonyId <= 0) return null;

        if (FuelService.getFuel(level, colonyId) <= 0) return null;

        double bonus = ColonyHeatParamsStorage.get(level).getParams(colonyId).bonus();
        if (bonus <= 0.0) return null;

        return new HeatInfo(colonyId, bonus);
    }


}
