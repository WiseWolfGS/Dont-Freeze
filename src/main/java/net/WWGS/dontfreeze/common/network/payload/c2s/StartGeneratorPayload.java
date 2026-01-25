package net.WWGS.dontfreeze.common.network.payload.c2s;

import net.WWGS.dontfreeze.DFConstants;
import net.WWGS.dontfreeze.common.block.entity.BlockGeneratorCoreEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record StartGeneratorPayload(BlockPos pos)
        implements CustomPacketPayload {

    public static final Type<StartGeneratorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DFConstants.MODID, "start_generator"));

    public static final StreamCodec<FriendlyByteBuf, StartGeneratorPayload> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeBlockPos(pkt.pos),
                    buf -> new StartGeneratorPayload(buf.readBlockPos())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartGeneratorPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(payload.pos());
            if (be instanceof BlockGeneratorCoreEntity core) {
                core.startBurn(200);
            }
        });
    }
}
