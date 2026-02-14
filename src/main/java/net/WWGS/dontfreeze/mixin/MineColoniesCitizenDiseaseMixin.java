package net.WWGS.dontfreeze.mixin;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenDiseaseHandler;
import com.mojang.logging.LogUtils;
import com.momosoftworks.coldsweat.api.util.Temperature;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CitizenDiseaseHandler.class, remap = false)
public abstract class MineColoniesCitizenDiseaseMixin {

    @Final
    @Shadow private ICitizenData citizenData;
    private static final Logger LOGGER = LogUtils.getLogger();

    @Redirect(
            method = "update(I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/api/colony/ICitizenData;getDiseaseModifier()D"
            ),
            remap = false
    )
    private double dontfreeze$snowBoostDiseaseModifier(ICitizenData data) {
        double base = data.getDiseaseModifier();

        var opt = data.getEntity();
        if (opt.isEmpty()) return base;

        if (dontFreeze$isFreezing(data)) {
            double snowMult = 2.0; // 눈일 때 2배 잘 걸리게(원하는 만큼 조절)
            return base * snowMult;
        }
        return base;
    }

//    @Inject(method = "update(I)V", at = @At("HEAD"))
//    private void dontfreeze$debug(int tickRate, CallbackInfo ci) {
//        var ent = citizenData.getEntity().orElse(null);
//        int count = citizenData.getColony().getCitizenManager().getCurrentCitizenCount();
//        LOGGER.info("[DontFreeze] ent={}, active={}, sick={}, job={}, count={}",
//                ent != null,
//                citizenData.getColony().isActive(),
//                (/* disease 여부는 shadow 없으니 간접적으로 */ citizenData.getCitizenDiseaseHandler().isSick()),
//                citizenData.getJob() == null ? "null" : citizenData.getJob().getClass().getSimpleName(),
//                count
//        );
//    }


    @Unique
    private static boolean dontFreeze$isFreezing(ICitizenData citizenData) {
        LOGGER.info("3");
        var entity = citizenData.getEntity().orElse(null);
        if (entity == null) return false;

        return Temperature.getTemperatures(entity).getOrDefault(Temperature.Trait.WORLD, 0.0) <= 0.8;

    }
}
