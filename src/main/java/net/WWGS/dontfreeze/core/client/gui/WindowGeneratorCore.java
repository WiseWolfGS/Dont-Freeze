package net.WWGS.dontfreeze.core.client.gui;

import net.WWGS.dontfreeze.DFConfig;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.colony.fuel.menu.MenuGeneratorCore;
import net.WWGS.dontfreeze.core.network.payload.C2SSetHeatBonus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public final class WindowGeneratorCore extends AbstractContainerScreen<MenuGeneratorCore> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "textures/gui/screen/townhallcore_gui.png");

    public WindowGeneratorCore(MenuGeneratorCore menu, Inventory inv, Component title) {
        super(menu, inv, title);

        this.imageWidth = 256;
        this.imageHeight = 256;

        this.titleLabelX = 58;
        this.titleLabelY = 58;
        this.inventoryLabelX = 58;
        this.inventoryLabelY = 130;
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

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int u = -50;
        int v = -50;
        gg.blit(TEX, x, y, u, v, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gg, int mouseX, int mouseY) {
        super.renderLabels(gg, mouseX, mouseY);

        int minutes = this.menu.getDisplayFuelMinutes();
        int seconds = this.menu.getDisplayFuelSeconds();

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
        double next = Mth.clamp(cur + delta, DFConfig.coreMinHeat, DFConfig.coreMaxHeat);
        int scaled = (int) Math.round(next * 100.0);

        PacketDistributor.sendToServer(
                new C2SSetHeatBonus(this.menu.getCorePos(), scaled)
        );
    }
}
