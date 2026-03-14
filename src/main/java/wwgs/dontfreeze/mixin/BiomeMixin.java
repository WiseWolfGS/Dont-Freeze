package wwgs.dontfreeze.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wwgs.dontfreeze.api.util.HeatedUtils;

@Mixin(Biome.class)
public abstract class BiomeMixin {

    @Inject(
            method = "shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void dontfreeze$shouldSnow(LevelReader level, BlockPos pos,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel sl && HeatedUtils.isHeatedNow(sl, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void dontfreeze$shouldFreeze3(LevelReader level, BlockPos pos, boolean mustBeAtEdge,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel sl && HeatedUtils.isHeatedNow(sl, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void dontfreeze$shouldFreeze2(LevelReader level, BlockPos pos,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel sl && HeatedUtils.isHeatedNow(sl, pos)) {
            cir.setReturnValue(false);
        }
    }
}
