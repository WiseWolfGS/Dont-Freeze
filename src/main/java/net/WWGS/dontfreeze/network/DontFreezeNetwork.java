package net.WWGS.dontfreeze.network;

import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.network.msg.S2CActiveGeneratorCores;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = DontFreeze.MODID)
public final class DontFreezeNetwork {
    private DontFreezeNetwork() {}

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar(DontFreeze.MODID).versioned("1");

        r.playToClient(
                S2CActiveGeneratorCores.TYPE,
                S2CActiveGeneratorCores.STREAM_CODEC,
                S2CActiveGeneratorCores::handle
        );
    }
}
