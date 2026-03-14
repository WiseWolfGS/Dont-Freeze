package wwgs.dontfreeze.core.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.core.network.payload.C2SCitizenMigrate;
import wwgs.dontfreeze.core.network.payload.C2SSetHeatBonus;
import wwgs.dontfreeze.core.network.payload.S2CActiveGeneratorCores;

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

        registrar.playToServer(
                C2SCitizenMigrate.TYPE,
                C2SCitizenMigrate.STREAM_CODEC,
                C2SCitizenMigrate::handle
        );
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
