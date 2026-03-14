package wwgs.dontfreeze.core.network.payload;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.core.network.entry.ColonyEntry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record C2SRequestColonyList() implements CustomPacketPayload {

    public static final Type<C2SRequestColonyList> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "c2s_request_colony_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestColonyList> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {},
                    buf -> new C2SRequestColonyList()
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final C2SRequestColonyList msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();

            if (!player.hasPermissions(2)) {
                // 목록 UI가 비게 보일 수 있으니 “빈 리스트” 내려주는 편이 UX는 좋음
                S2CColonyList reply = new S2CColonyList(List.of());
                ctx.reply(reply);
                return;
            }

            ServerLevel level = (ServerLevel) player.level();

            List<ColonyEntry> entries = new ArrayList<>();
            for (IColony c : getColoniesInLevel(level)) {
                // 이름 getter는 버전에 따라 다를 수 있어서 안전하게 처리
                String name = safeColonyName(c);
                entries.add(new ColonyEntry(c.getID(), name));
            }

            ctx.reply(new S2CColonyList(entries));
        });
    }

    private static Collection<IColony> getColoniesInLevel(ServerLevel level) {
        IColonyManager mgr = IColonyManager.getInstance();

        try {
            Method m = mgr.getClass().getMethod("getColonies", level.getClass());
            Object res = m.invoke(mgr, level);
            if (res instanceof Collection<?> col) {
                @SuppressWarnings("unchecked")
                Collection<IColony> cast = (Collection<IColony>) col;
                return cast;
            }
        } catch (Exception ignored) {}

        try {
            Method m = mgr.getClass().getMethod("getAllColonies");
            Object res = m.invoke(mgr);
            if (res instanceof Collection<?> col) {
                @SuppressWarnings("unchecked")
                Collection<IColony> cast = (Collection<IColony>) col;
                return cast;
            }
        } catch (Exception ignored) {}

        return List.of();
    }

    private static String safeColonyName(IColony colony) {
        try {
            Method m = colony.getClass().getMethod("getName");
            Object res = m.invoke(colony);
            if (res != null) return res.toString();
        } catch (Exception ignored) {}

        return "Colony " + colony.getID();
    }
}