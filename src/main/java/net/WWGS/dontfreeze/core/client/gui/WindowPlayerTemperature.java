package net.WWGS.dontfreeze.core.client.gui;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.WWGS.dontfreeze.Dontfreeze;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import static java.lang.Math.clamp;

@EventBusSubscriber(modid = Dontfreeze.MODID, value = Dist.CLIENT)
public final class WindowPlayerTemperature {
    private WindowPlayerTemperature() {}

    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "body_temp_hud");

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (mc.options.hideGui) return; // F1 숨김 존중
            if (!mc.player.isShiftKeyDown()) return;

            double temp = Temperature.get(mc.player, Temperature.Trait.BODY);
            double csTemp = clamp(temp / 150.0, -1.0, 1.0);
            csTemp = Math.tanh(2.0 * csTemp) / Math.tanh(2.0);
            csTemp = 37.0 + csTemp * 5.0;
            String text = String.format("체온: %.2f", csTemp);
            int x = 8;
            int y = 200;

            guiGraphics.drawString(
                    mc.font,
                    String.format(text),
                    x, y,
                    0xFFFFFF
            );
        });
    }
}
