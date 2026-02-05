package net.WWGS.dontfreeze.mixin;

import net.WWGS.dontfreeze.api.colony.ColonyQuery;
import net.WWGS.dontfreeze.api.util.QueryUtils;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelService;
import net.WWGS.dontfreeze.core.colony.heat.storage.ColonyHeatParamsStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowLayerBlock.class)
public abstract class SnowLayerBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true, remap = false)
    private void dontfreeze$noSnowInHeatedArea(
            BlockState state, LevelReader level, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (isHeatedNow(serverLevel, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), remap = false)
    private void dontfreeze$meltSnowLayers(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource rand,
            CallbackInfo ci
    ) {
        if (!isHeatedNow(level, pos)) return;

        // bonus가 높을수록 잘 녹게 (0.0~5.0 기준)
        double bonus = ColonyHeatParamsStorage.get(level)
                .getParams(QueryUtils.colonyQuery().findColonyIdAtPos(level, pos))
                .bonus();

        // 확률: 최소 10% ~ 최대 80%
        double chance = Math.min(0.80, 0.10 + bonus * 0.10);
        if (rand.nextDouble() > chance) return;

        if (!state.hasProperty(SnowLayerBlock.LAYERS)) return;

        int layers = state.getValue(SnowLayerBlock.LAYERS);
        if (layers > 1) {
            level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, layers - 1), 3);
        } else {
            level.removeBlock(pos, false);
        }
    }

    private static boolean isHeatedNow(ServerLevel level, BlockPos pos) {
        ColonyQuery cq = QueryUtils.colonyQuery();
        if (cq == null) return false;

        Integer colonyId = cq.findColonyIdAtPos(level, pos);
        if (colonyId == null) return false;

        if (FuelService.getFuel(level, colonyId) <= 0) return false;

        double bonus = ColonyHeatParamsStorage.get(level).getParams(colonyId).bonus();
        return bonus > 0.0;
    }
}