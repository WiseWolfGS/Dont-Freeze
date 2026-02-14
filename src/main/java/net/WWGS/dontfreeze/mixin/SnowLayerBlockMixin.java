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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowLayerBlock.class)
public abstract class SnowLayerBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true, remap = false)
    private void dontfreeze$canSurvive(
            BlockState state, LevelReader level, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (level instanceof ServerLevel sl && dontfreeze$isHeatedNow(sl, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void dontfreeze$randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd,
            CallbackInfo ci
    ) {
        if (dontfreeze$isHeatedNow(level, pos)) {
            level.removeBlock(pos, false);
            ci.cancel();
        }
    }

    @Unique
    private static boolean dontfreeze$isHeatedNow(ServerLevel level, BlockPos pos) {
        ColonyQuery cq = QueryUtils.colonyQuery();
        if (cq == null) return false;

        Integer colonyId = cq.findColonyIdAtPos(level, pos);
        if (colonyId == null || colonyId <= 0) return false;

        if (FuelService.getFuel(level, colonyId) <= 0) return false;

        double bonus = ColonyHeatParamsStorage.get(level).getParams(colonyId).bonus();
        return bonus > 0.0;
    }
}
