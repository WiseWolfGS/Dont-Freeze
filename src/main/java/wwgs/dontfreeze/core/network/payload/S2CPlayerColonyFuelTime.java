package wwgs.dontfreeze.core.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.core.client.state.ClientColonyFuelTimeState;

public record S2CPlayerColonyFuelTime(int colonyId, int minutes, int seconds, double tickPerFuel) implements CustomPacketPayload {
    public static final Type<S2CPlayerColonyFuelTime> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "s2c_player_colony_fuel_time"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlayerColonyFuelTime> STREAM_CODEC =
            StreamCodec.of(S2CPlayerColonyFuelTime::encode, S2CPlayerColonyFuelTime::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, S2CPlayerColonyFuelTime msg) {
        buf.writeVarInt(msg.colonyId);
        buf.writeVarInt(msg.minutes);
        buf.writeVarInt(msg.seconds);
        buf.writeDouble(msg.tickPerFuel);
    }

    private static S2CPlayerColonyFuelTime decode(RegistryFriendlyByteBuf buf) {
        int colonyId = buf.readVarInt();
        int minutes = buf.readVarInt();
        int seconds = buf.readVarInt();
        double tickPerFuel = buf.readDouble();
        return new S2CPlayerColonyFuelTime(colonyId, minutes, seconds, tickPerFuel);
    }

    public static void handle(S2CPlayerColonyFuelTime msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientColonyFuelTimeState.update(msg.colonyId, msg.minutes, msg.seconds, msg.tickPerFuel));
    }
}
