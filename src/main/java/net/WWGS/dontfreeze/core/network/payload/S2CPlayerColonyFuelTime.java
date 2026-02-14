package net.WWGS.dontfreeze.core.network.payload;

import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.client.state.ClientColonyFuelTimeState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 서버에서 플레이어에게 "내가 소속된 콜로니"의 발전기(코어) 남은 연료 시간을 동기화.
 *
 * colonyId가 0 이하이면(미소속/조회 실패) 분/초는 0으로 간주.
 */
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
