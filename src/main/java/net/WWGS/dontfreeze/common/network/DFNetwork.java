package net.WWGS.dontfreeze.common.network;

import net.WWGS.dontfreeze.DFConstants;
import net.WWGS.dontfreeze.common.network.payload.c2s.SetHeatBonusPayload;
import net.WWGS.dontfreeze.common.network.payload.c2s.StartGeneratorPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class DFNetwork {
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(DFConstants.MODID);

        registrar.playToServer(
                StartGeneratorPayload.TYPE,
                StartGeneratorPayload.CODEC,
                StartGeneratorPayload::handle
        );
        registrar.playToServer(
                SetHeatBonusPayload.TYPE,
                SetHeatBonusPayload.STREAM_CODEC,
                SetHeatBonusPayload::handle
        );
    }
}
