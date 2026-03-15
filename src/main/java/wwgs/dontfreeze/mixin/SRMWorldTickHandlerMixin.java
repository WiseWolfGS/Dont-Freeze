package wwgs.dontfreeze.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wwgs.dontfreeze.api.util.HeatedUtils;

@Mixin(value = snownee.snow.WorldTickHandler.class, remap = false)
public class SRMWorldTickHandlerMixin {

    @Inject(
            method = "snowHereIfPossible",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void dontfreeze$cancelSnowHereIfPossible(
            ServerLevel level, BlockPos.MutableBlockPos pos, BlockState blockState, CallbackInfoReturnable<Boolean> cir
    ) {
        if (HeatedUtils.isHeatedNow(level, pos)) {
            cir.cancel();
        }
    }
}