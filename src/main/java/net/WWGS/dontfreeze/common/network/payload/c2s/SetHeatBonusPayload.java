package net.WWGS.dontfreeze.common.network.payload.c2s;

import net.WWGS.dontfreeze.DFConstants;
import net.WWGS.dontfreeze.common.menu.GeneratorCoreMenu;
import net.WWGS.dontfreeze.feature.heat.storage.ColonyHeatParamsStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetHeatBonusPayload(BlockPos corePos, int bonusScaled) implements CustomPacketPayload {
    public static final Type<SetHeatBonusPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DFConstants.MODID, "set_heat_bonus"));

    public static final StreamCodec<FriendlyByteBuf, SetHeatBonusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetHeatBonusPayload::corePos,
                    ByteBufCodecs.VAR_INT, SetHeatBonusPayload::bonusScaled,
                    SetHeatBonusPayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetHeatBonusPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!(sp.containerMenu instanceof GeneratorCoreMenu menu)) return;
            if (!menu.getBlockPos().equals(payload.corePos())) return;

            int colonyId = menu.getColonyId();
            if (colonyId < 0) return;

            double bonus = payload.bonusScaled() / 100.0;
            ColonyHeatParamsStorage
                    .get((ServerLevel) sp.level())
                    .setBonus(colonyId, bonus);
        });
    }
}
