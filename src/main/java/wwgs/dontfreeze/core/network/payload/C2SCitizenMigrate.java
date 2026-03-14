package wwgs.dontfreeze.core.network.payload;

import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.entity.citizen.AbstractCivilianEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.core.MineColoniesCitizenQuery;

public record C2SCitizenMigrate(int citizenEntityId, int targetColonyId) implements CustomPacketPayload {

    public static final Type<C2SCitizenMigrate> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "c2s_citizen_migrate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCitizenMigrate> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        buf.writeInt(msg.citizenEntityId);
                        buf.writeInt(msg.targetColonyId);
                    },
                    buf -> new C2SCitizenMigrate(buf.readInt(), buf.readInt())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final C2SCitizenMigrate msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();

            if (!player.hasPermissions(2)) {
                player.displayClientMessage(Component.literal("권한이 없습니다."), false);
                return;
            }

            ServerLevel level = (ServerLevel) player.level();

            var entity = level.getEntity(msg.citizenEntityId());
            if (!(entity instanceof AbstractCivilianEntity civilianEntity)) {
                player.displayClientMessage(Component.literal("대상 엔티티가 시민이 아닙니다."), false);
                return;
            }

            ICivilianData data = civilianEntity.getCivilianData();
            if (data == null) {
                player.displayClientMessage(Component.literal("시민 데이터가 없습니다."), false);
                return;
            }

            IColony targetColony = IColonyManager.getInstance()
                    .getColonyByWorld(msg.targetColonyId(), level);

            if (targetColony == null) {
                player.displayClientMessage(Component.literal("대상 콜로니를 찾을 수 없습니다: " + msg.targetColonyId), false);
                return;
            }

            try {
                new MineColoniesCitizenQuery().migrateCitizen(targetColony, data);
                player.displayClientMessage(
                        Component.literal("이적 완료: " + civilianEntity.getName().getString() + " -> 콜로니 " + msg.targetColonyId),
                        false
                );
            } catch (Exception ex) {
                player.displayClientMessage(Component.literal("이적 실패: " + ex.getMessage()), false);
            }
        });
    }
}
