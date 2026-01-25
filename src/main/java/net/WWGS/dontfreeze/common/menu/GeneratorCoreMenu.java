package net.WWGS.dontfreeze.common.menu;

import net.WWGS.dontfreeze.common.api.IntegrationAPIs;
import net.WWGS.dontfreeze.common.block.entity.BlockGeneratorCoreEntity;
import net.WWGS.dontfreeze.common.registry.DFMenus;
import net.WWGS.dontfreeze.feature.heat.storage.ColonyHeatParamsStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GeneratorCoreMenu extends AbstractContainerMenu {
    private final BlockGeneratorCoreEntity be;
    private static final int SECONDS_PER_MINUTE = 60;

    private final Inventory playerInv;

    private final BlockPos corePos;
    private int colonyId = -1;

    private final DataSlot colonyIdSlot = DataSlot.standalone();
    private final DataSlot heatBonusScaled = DataSlot.standalone();

    public GeneratorCoreMenu(int containerId, Inventory inv, BlockGeneratorCoreEntity be) {
        super(DFMenus.MENU_GENERATOR_CORE.get(), containerId);
        this.playerInv = inv;
        this.corePos = be.getBlockPos();
        this.be = be;

        DataSlot fuelMinutes = DataSlot.standalone();
        this.addDataSlot(fuelMinutes);
        DataSlot fuelSeconds = DataSlot.standalone();
        this.addDataSlot(fuelSeconds);

        this.addDataSlot(colonyIdSlot);
        this.addDataSlot(heatBonusScaled);

        addPlayerInventory(inv, 58, 141);
        addHotbar(inv, 58, 199);

        computeColonyIdIfServer();
    }

    private void addPlayerInventory(Inventory inv, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = left + col * 18;
                int y = top + row * 18;
                this.addSlot(new Slot(inv, 9 + row * 9 + col, x, y));
            }
        }
    }

    private void addHotbar(Inventory inv, int left, int top) {
        for (int col = 0; col < 9; col++) {
            int x = left + col * 18;
            int y = top;
            this.addSlot(new Slot(inv, col, x, y));
        }
    }

    private void computeColonyIdIfServer() {
        Player player = this.playerInv.player;
        if (!(player.level() instanceof ServerLevel level)) return;

        var cq = IntegrationAPIs.colonyQuery();
        if (cq == null) {
            colonyId = -1;
            colonyIdSlot.set(colonyId);
            return;
        }

        Integer id = cq.findColonyIdAt(level, corePos);
        if (id != null) {
            colonyId = id;
            colonyIdSlot.set(colonyId);
            return;
        }

        var ref = cq.findNearestTownHall(level, corePos);
        colonyId = (ref == null) ? -1 : ref.colonyId();
        colonyIdSlot.set(colonyId);
    }

    private void syncHeatBonusIfServer() {
        if (!(this.playerInv.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (this.colonyId < 0) {
            heatBonusScaled.set(0);
            return;
        }

        double bonus =
                ColonyHeatParamsStorage.get(level).getParams(colonyId).bonus();

        heatBonusScaled.set((int) Math.round(bonus * 100));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return empty;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int fuelSlotIndex = 0;
        int playerInvStart = 1;
        int playerInvEnd = this.slots.size();

        if (index == fuelSlotIndex) {
            if (!this.moveItemStackTo(stack, playerInvStart, playerInvEnd, true)) return empty;
        } else {
            if (!this.moveItemStackTo(stack, fuelSlotIndex, fuelSlotIndex + 1, false)) return empty;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    public boolean isActive() {
        return be.isActive();
    }

    public int getBurnTickMinutes() {
        return be.getBurnTicks() / 20 / SECONDS_PER_MINUTE;
    }

    public int getBurnTickSeconds() {
        return be.getBurnTicks() / 20 % SECONDS_PER_MINUTE;
    }

    public BlockPos getBlockPos() {
        return be.getBlockPos();
    }

    public int getColonyId() {
        return colonyIdSlot.get();
    }

    public double getHeatBonus() {
        return heatBonusScaled.get() / 100.0;
    }


    // 테스트
    public void startTestBurn() {
        if (be != null) {
            be.startBurn(200); // 10초 테스트
        }
    }
}
