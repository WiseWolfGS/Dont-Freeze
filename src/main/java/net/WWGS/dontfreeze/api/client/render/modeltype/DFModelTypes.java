package net.WWGS.dontfreeze.api.client.render.modeltype;

import com.minecolonies.api.client.render.modeltype.IModelType;
import net.WWGS.dontfreeze.Dontfreeze;
import net.minecraft.resources.ResourceLocation;

public final class DFModelTypes {
    public static final ResourceLocation FIREKEEPER_MODEL_ID = ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "firekeeper");

    public static IModelType FIREKEEPER;

    private DFModelTypes() {
        throw new IllegalStateException("Tried to initialize: DFModelTypes but this is a Utility class.");
    }
}
