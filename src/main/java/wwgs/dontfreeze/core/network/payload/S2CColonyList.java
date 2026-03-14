package wwgs.dontfreeze.core.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.core.client.DFClientState;
import wwgs.dontfreeze.core.network.entry.ColonyEntry;

import java.util.ArrayList;
import java.util.List;

public record S2CColonyList(List<ColonyEntry> colonies) implements CustomPacketPayload {

    public static final Type<S2CColonyList> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "s2c_colony_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CColonyList> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        buf.writeInt(msg.colonies.size());
                        for (ColonyEntry e : msg.colonies) {
                            buf.writeInt(e.id());
                            buf.writeUtf(e.name());
                        }
                    },
                    buf -> {
                        int size = buf.readInt();
                        List<ColonyEntry> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            int id = buf.readInt();
                            String name = buf.readUtf();
                            list.add(new ColonyEntry(id, name));
                        }
                        return new S2CColonyList(list);
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final S2CColonyList msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            DFClientState.setColonyList(msg.colonies);
        });
    }
}
