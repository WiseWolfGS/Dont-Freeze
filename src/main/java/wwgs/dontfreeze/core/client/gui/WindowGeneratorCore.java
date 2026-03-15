package wwgs.dontfreeze.core.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import wwgs.dontfreeze.Config;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.core.network.payload.C2SSetHeatBonus;
import wwgs.dontfreeze.core.temperature.fuel.menu.MenuGeneratorCore;

public final class WindowGeneratorCore extends AbstractContainerScreen<MenuGeneratorCore> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, "textures/gui/screen/townhallcore_gui.png");

    public WindowGeneratorCore(MenuGeneratorCore menu, Inventory inv, Component title) {
        super(menu, inv, title);

        this.imageWidth = 190;
        this.imageHeight = 244;

        this.titleLabelX = 14;
        this.titleLabelY = 14;

        this.inventoryLabelX = 14;
        this.inventoryLabelY = 142;
    }

    @Override
    protected void init() {
        super.init();

        int bx = leftPos + 70;
        int by = topPos + 123;

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
        int x = this.leftPos;
        int y = this.topPos;

        gg.blit(
                TEX,
                x, y,
                0, 0,
                this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight
        );
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gg, int mouseX, int mouseY) {
        super.renderLabels(gg, mouseX, mouseY);

        int minutes = this.menu.getDisplayFuelMinutes();
        int seconds = this.menu.getDisplayFuelSeconds();

        String text = minutes + "분 " + seconds + "초";
        int tx = 22;
        int ty = 26;

        gg.drawString(this.font, "연료: " + text, tx, ty, 0x404040, false);
        gg.drawString(this.font, String.format("열 보너스: %.2f", this.menu.getHeatBonus()), tx, ty + 10, 0x404040, false);
        gg.drawString(this.font, String.format("건물 가중치: %d (건물 레벨 합: %d)", this.menu.getBuildingCount(), this.menu.getBuildingLevelSum()), tx, ty + 20, 0x404040, false);


        int cps = this.menu.getCostPerSecond();
        if (cps > 0) {
            gg.drawString(this.font, "비용/초: " + cps, tx, ty + 30, 0x404040, false);

            String coalText = this.menu.getCoalEfficiencyMinutes() + "분 " + this.menu.getCoalEfficiencySeconds() + "초";
            gg.drawString(this.font, "1 석탄당 작동 시간 -> " + coalText, tx, ty + 40, 0x404040, false);
        }
    }

    private void changeBonus(double delta) {
        double cur = this.menu.getHeatBonus();
        double next = Mth.clamp(cur + delta, Config.coreMinHeat, Config.coreMaxHeat);
        int scaled = (int) Math.round(next * 100.0);

        PacketDistributor.sendToServer(
                new C2SSetHeatBonus(this.menu.getCorePos(), scaled)
        );
    }
}