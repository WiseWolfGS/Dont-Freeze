package net.WWGS.dontfreeze.core.network.payload;

import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.client.state.ClientActiveCoreState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public record S2CActiveGeneratorCores(Set<BlockPos> cores) implements CustomPacketPayload {
    public static final Type<S2CActiveGeneratorCores> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "s2c_active_generator_cores"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CActiveGeneratorCores> STREAM_CODEC =
            StreamCodec.of(S2CActiveGeneratorCores::encode, S2CActiveGeneratorCores::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, S2CActiveGeneratorCores msg) {
        buf.writeVarInt(msg.cores.size());
        for (BlockPos pos : msg.cores) {
            buf.writeBlockPos(pos);
        }
    }

    private static S2CActiveGeneratorCores decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<BlockPos> set = new HashSet<>(Math.max(16, size * 2));
        for (int i = 0; i < size; i++) {
            set.add(buf.readBlockPos());
        }
        return new S2CActiveGeneratorCores(set);
    }

    public static void handle(S2CActiveGeneratorCores msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientActiveCoreState.replace(msg.cores));
    }
}
