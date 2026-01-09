package net.WWGS.dontfreeze.client.gui;

import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.domain.fuel.menu.ColonyFuelMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ColonyFuelScreen extends AbstractContainerScreen<ColonyFuelMenu> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(DontFreeze.MODID, "textures/gui/screen/townhallcore_gui.png");

    public ColonyFuelScreen(ColonyFuelMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);

        // 기본 컨테이너 UI 크기(필요하면 조정)
        this.imageWidth = 256;
        this.imageHeight = 256; // 인벤토리 포함 기본 높이

        // 라벨 위치/표시 원하면 여기서 조정 가능
        this.titleLabelX = 58;
        this.titleLabelY = 58;
        this.inventoryLabelX = 58;
        this.inventoryLabelY = 130;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int u = -50;  // <- 프레임 시작 X(픽셀)
        int v = -50;  // <- 프레임 시작 Y(픽셀)
        gg.blit(TEX, x, y, u, v, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gg, int mouseX, int mouseY) {
        super.renderLabels(gg, mouseX, mouseY);

        int ticks = this.menu.getDisplayFuelTicks();

        // 보기 좋게 변환
        int seconds = ticks / 20;
        int minutes = seconds / 60;

        String text = minutes + "m " + (seconds % 60) + "s";

        gg.drawString(this.font, "Fuel: " + text, 100, 130, 0x404040, false);
    }
}
