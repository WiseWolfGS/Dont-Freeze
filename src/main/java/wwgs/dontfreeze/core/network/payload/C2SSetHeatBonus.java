package wwgs.dontfreeze.core.network.payload;

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
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.core.temperature.fuel.menu.MenuGeneratorCore;
import wwgs.dontfreeze.core.temperature.heat.HeatBonusSavedData;

public record C2SSetHeatBonus(BlockPos corePos, int bonusScaled) implements CustomPacketPayload {

    public static final Type<C2SSetHeatBonus> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "set_heat_bonus"));

    public static final StreamCodec<FriendlyByteBuf, C2SSetHeatBonus> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, C2SSetHeatBonus::corePos,
                    ByteBufCodecs.VAR_INT, C2SSetHeatBonus::bonusScaled,
                    C2SSetHeatBonus::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SSetHeatBonus msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!(sp.containerMenu instanceof MenuGeneratorCore menu)) return;
            if (!menu.getCorePos().equals(msg.corePos())) return;

            int colonyId = menu.getColonyId();
            if (colonyId < 0) return;

            double bonus = msg.bonusScaled() / 100.0;
            HeatBonusSavedData.get((ServerLevel) sp.level()).setBonus(colonyId, bonus);
        });
    }
}