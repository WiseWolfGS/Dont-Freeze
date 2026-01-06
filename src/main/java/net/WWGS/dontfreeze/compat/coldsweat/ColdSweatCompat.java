package net.WWGS.dontfreeze.compat.coldsweat;

import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.compat.coldsweat.modifier.ChunkWarmthModifier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;

@EventBusSubscriber(modid = DontFreeze.MODID)
public final class ColdSweatCompat {

    public static final ResourceLocation CHUNK_WARMTH_ID = ResourceLocation.fromNamespaceAndPath(DontFreeze.MODID, "chunk_warmth");

    private ColdSweatCompat() {}

    public static void init() {
        DontFreeze.LOGGER.info("ColdSweatCompat initialized");
    }

    @SubscribeEvent
    public static void onRegisterTempModifiers(TempModifierRegisterEvent event) {
        event.register(CHUNK_WARMTH_ID, ChunkWarmthModifier::new);
    }
}
