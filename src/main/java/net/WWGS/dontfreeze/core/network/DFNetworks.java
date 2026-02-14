package net.WWGS.dontfreeze.core.network;

import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.network.payload.C2SSetHeatBonus;
import net.WWGS.dontfreeze.core.network.payload.S2CActiveGeneratorCores;
import net.WWGS.dontfreeze.core.network.payload.S2CPlayerColonyFuelTime;
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
        registrar.playToClient(
                S2CPlayerColonyFuelTime.TYPE,
                S2CPlayerColonyFuelTime.STREAM_CODEC,
                S2CPlayerColonyFuelTime::handle
        );
        registrar.playToServer(
                C2SSetHeatBonus.TYPE,
                C2SSetHeatBonus.STREAM_CODEC,
                C2SSetHeatBonus::handle
        );
    }
}
