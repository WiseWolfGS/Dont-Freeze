package net.WWGS.dontfreeze.network.msg;

import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.client.state.ClientHeaterState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

public record S2CActiveGeneratorCores(Set<BlockPos> cores) implements CustomPacketPayload {
    public static final Type<S2CActiveGeneratorCores> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DontFreeze.MODID, "s2c_active_generator_cores"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CActiveGeneratorCores> STREAM_CODEC =
            StreamCodec.of(S2CActiveGeneratorCores::encode, S2CActiveGeneratorCores::decode);

    private static void encode(RegistryFriendlyByteBuf buf, S2CActiveGeneratorCores msg) {
        buf.writeVarInt(msg.cores.size());
        for (BlockPos p : msg.cores) {
            buf.writeBlockPos(p);
        }
    }

    private static S2CActiveGeneratorCores decode(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Set<BlockPos> set = new HashSet<>(Math.max(16, n * 2));
        for (int i = 0; i < n; i++) {
            set.add(buf.readBlockPos());
        }
        return new S2CActiveGeneratorCores(set);
    }

    public static void handle(S2CActiveGeneratorCores msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientHeaterState.replaceActiveGeneratorCores(msg.cores());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
