package net.WWGS.dontfreeze.client.gui;

import net.WWGS.dontfreeze.DFConstants;
import net.WWGS.dontfreeze.common.config.DFConfigValues;
import net.WWGS.dontfreeze.common.menu.GeneratorCoreMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public final class GeneratorCoreScreen extends AbstractContainerScreen<GeneratorCoreMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(DFConstants.MODID, "textures/gui/screen/townhallcore_gui.png");

    public GeneratorCoreScreen(GeneratorCoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 256;
        this.imageHeight = 256;

        this.titleLabelX = 58;
        this.titleLabelY = 58;
        this.inventoryLabelX = 58;
        this.inventoryLabelY = 130;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int u = -50;
        int v = -50;
        guiGraphics.blit(TEXTURE, x, y, u, v, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gg, int mouseX, int mouseY) {
        super.renderLabels(gg, mouseX, mouseY);

        int minutes = this.menu.getBurnTickMinutes();
        int seconds = this.menu.getBurnTickSeconds();

        String text = minutes + "m " + seconds + "s";
        gg.drawString(this.font, "Fuel: " + text, 100, 58, 0x404040, false);

        gg.drawString(
                this.font,
                String.format("Heat Bonus: %.2f", this.menu.getHeatBonus()),
                100, 68, 0x404040, false
        );
    }

    private void changeBonus(double delta) {
        double cur = this.menu.getHeatBonus();
        double next = Mth.clamp(cur + delta, DFConfigValues.coreMinHeat, DFConfigValues.coreMaxHeat);
        int scaled = (int) Math.round(next * 100.0);
    }

    @Override
    protected void init() {
        super.init();

        int bx = leftPos + 100;
        int by = topPos + 78;

        this.addRenderableWidget(
                Button.builder(Component.literal("-"), b -> changeBonus(-0.25))
                        .bounds(bx, by, 20, 20).build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("+"), b -> changeBonus(+0.25))
                        .bounds(bx + 24, by, 20, 20).build()
        );
    }
}
