package net.WWGS.dontfreeze.mixin;

import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.WWGS.dontfreeze.domain.heat.service.HeatedArea;
import net.WWGS.dontfreeze.domain.heat.storage.ColonyHeatStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowLayerBlock.class)
public abstract class SnowLayerBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true, remap = false)
    private void dontfreeze$noSnowInHeatedArea(
            BlockState state, LevelReader level, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (HeatedArea.isHeated(serverLevel, pos)) {
            cir.setReturnValue(false);
        }
    }
}
