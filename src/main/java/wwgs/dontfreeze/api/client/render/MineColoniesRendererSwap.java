package wwgs.dontfreeze.api.client.render;

import com.minecolonies.core.client.render.mobs.norsemen.RendererArcherNorsemen;
import com.minecolonies.core.client.render.mobs.norsemen.RendererChiefNorsemen;
import com.minecolonies.core.client.render.mobs.norsemen.RendererShieldmaidenNorsemen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import wwgs.dontfreeze.Dontfreeze;

@EventBusSubscriber(modid = Dontfreeze.MODID,  value = Dist.CLIENT)
public class MineColoniesRendererSwap {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        swap(event, id("minecolonies", "archerbarbarian"), RendererArcherNorsemen::new);
        swap(event, id("minecolonies", "chiefbarbarian"),  RendererChiefNorsemen::new);
        swap(event, id("minecolonies", "barbarian"),       RendererShieldmaidenNorsemen::new);

        swap(event, id("minecolonies", "camparcherbarbarian"), RendererArcherNorsemen::new);
        swap(event, id("minecolonies", "campchiefbarbarian"),  RendererChiefNorsemen::new);
        swap(event, id("minecolonies", "campbarbarian"),       RendererShieldmaidenNorsemen::new);
    }

    private static ResourceLocation id(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    private static <T extends Entity> void swap(
            EntityRenderersEvent.RegisterRenderers event,
            ResourceLocation entityId,
            EntityRendererProvider<T> renderer
    ) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            Dontfreeze.LOGGER.warn("MineColoniesRendererSwap: EntityType not found: {}", entityId);
            return;
        }

        @SuppressWarnings("unchecked")
        EntityType<T> type = (EntityType<T>) BuiltInRegistries.ENTITY_TYPE.get(entityId);

        event.registerEntityRenderer(type, renderer);
        Dontfreeze.LOGGER.info("MineColoniesRendererSwap: Swapped renderer for {}", entityId);
    }
}