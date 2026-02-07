package net.WWGS.dontfreeze.core.colony.heat.service;

import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.colony.heat.platform.ColonyHeatModifier;
import net.WWGS.dontfreeze.core.colony.heat.platform.WorldHeatSmoothingModifier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Dontfreeze.MODID)
public final class HeatModifierRegistry {
    public static final ResourceLocation TEMP_SMOOTH_ID = ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "temp_smooth");
    public static final ResourceLocation COLONY_WARMTH_ID = ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "colony_warmth");

    @SubscribeEvent
    public static void onRegisterTempModifiers(TempModifierRegisterEvent event) {
        event.register(COLONY_WARMTH_ID, ColonyHeatModifier::new);
        event.register(TEMP_SMOOTH_ID, WorldHeatSmoothingModifier::new);
    }
}
