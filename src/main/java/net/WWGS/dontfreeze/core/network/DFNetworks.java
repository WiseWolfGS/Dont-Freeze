package net.WWGS.dontfreeze.core.network;

import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.network.payload.C2SSetHeatBonus;
import net.WWGS.dontfreeze.core.network.payload.S2CActiveGeneratorCores;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DFNetworks {
    private DFNetworks() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Dontfreeze.MODID);
        registrar.playToClient(
                S2CActiveGeneratorCores.TYPE,
                S2CActiveGeneratorCores.STREAM_CODEC,
                S2CActiveGeneratorCores::handle
        );
        registrar.playToServer(
                C2SSetHeatBonus.TYPE,
                C2SSetHeatBonus.STREAM_CODEC,
                C2SSetHeatBonus::handle
        );
    }
}
