package net.WWGS.dontfreeze.core.colony.fuel.menu;

import net.WWGS.dontfreeze.DFConfig;
import net.WWGS.dontfreeze.api.client.gui.DFMenus;
import net.WWGS.dontfreeze.api.util.QueryUtils;
import net.WWGS.dontfreeze.core.colony.fuel.platform.FuelSavedData;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelBurnTime;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelService;
import net.WWGS.dontfreeze.core.colony.heat.storage.ColonyHeatParamsStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MenuGeneratorCore extends AbstractContainerMenu {
    private static final int FUEL_SLOT_X = 130;
    private static final int FUEL_SLOT_Y = 112;

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int BONUS_SCALE = 100;

    private final Inventory playerInv;
    private final Container fuelSlot;

    private final BlockPos corePos;
    private int colonyId = -1;

    private boolean handlingFuelSlot = false;
    private long lastConsumeServerTick = -1;

    private final DataSlot colonyIdSlot = DataSlot.standalone();
    private final DataSlot fuelMinutes = DataSlot.standalone();
    private final DataSlot fuelSeconds = DataSlot.standalone();
    private final DataSlot heatBonusScaled = DataSlot.standalone();

    public MenuGeneratorCore(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, buf.readBlockPos());
    }

    public MenuGeneratorCore(int containerId, Inventory inv, BlockPos corePos) {
        super(DFMenus.MENU_GENERATOR_CORE.get(), containerId);
        this.playerInv = inv;
        this.corePos = corePos;

        this.addDataSlot(colonyIdSlot);
        this.addDataSlot(fuelMinutes);
        this.addDataSlot(fuelSeconds);
        this.addDataSlot(heatBonusScaled);

        this.fuelSlot = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                MenuGeneratorCore.this.onFuelSlotChanged();
            }
        };

        this.addSlot(new Slot(this.fuelSlot, 0, FUEL_SLOT_X, FUEL_SLOT_Y));

        addPlayerInventorySlots(inv, 58, 141);
        addHotbarSlots(inv, 58, 199);

        computeColonyIdIfServer();
        syncFuelToDataSlotsIfServer();
    }

    private void onFuelSlotChanged() {

    }

    private void addPlayerInventorySlots(Inventory inv, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = left + col * 18;
                int y = top + row * 18;
                this.addSlot(new Slot(inv, 9 + row * 9 + col, x, y));
            }
        }
    }

    private void addHotbarSlots(Inventory inv, int left, int top) {
        for (int col = 0; col < 9; col++) {
            int x = left + col * 18;
            int y = top;
            this.addSlot(new Slot(inv, col, x, y));
        }
    }

    private void computeColonyIdIfServer() {
        Player player = this.playerInv.player;
        if (!(player.level() instanceof ServerLevel level)) return;

        var cq = QueryUtils.colonyQuery();
        if (cq == null) {
            colonyId = -1;
            colonyIdSlot.set(colonyId);
            return;
        }

        colonyId = cq.findColonyIdAtPos(level, corePos);
        colonyIdSlot.set(colonyId);
    }

    private void syncFuelToDataSlotsIfServer() {
        Player player = this.playerInv.player;
        if (!(player.level() instanceof ServerLevel level)) return;

        int fuel = (colonyId < 0) ? 0 : FuelSavedData.get(level).getFuel(colonyId);
        if (fuel <= 0) return;

        double bonus = getHeatBonus();

        int costPerSecond = DFConfig.baseCostOfCore + (int)Math.ceil(bonus * DFConfig.bonusCostOfCore);
        if (costPerSecond <= 0) return;

        int seconds = fuel / costPerSecond;

        int minutesPart = seconds / SECONDS_PER_MINUTE;
        int secondsPart = seconds % SECONDS_PER_MINUTE;

        fuelMinutes.set(minutesPart);
        fuelSeconds.set(secondsPart);
    }

    private void syncHeatBonusIfServer() {
        if (!(this.playerInv.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (this.colonyId < 0) {
            heatBonusScaled.set(0);
            return;
        }

        double bonus = ColonyHeatParamsStorage.get(level).getParams(colonyId).bonus();

        heatBonusScaled.set((int) Math.round(bonus * BONUS_SCALE));
    }

    private void tryConsumeFuelOnePerTick(ServerLevel level) {
        if (colonyId < 0) return;

        long now = level.getGameTime();
        if (now == lastConsumeServerTick) return;

        if (handlingFuelSlot) return;
        ItemStack stack = this.fuelSlot.getItem(0);
        if (stack.isEmpty() || !DFConfig.fuelItems.contains(stack.getItem())) return;

        int burnTicks = FuelBurnTime.getBurnTicks(stack);
        if (burnTicks <= 0) return;

        handlingFuelSlot = true;
        try {
            stack.shrink(1);
            if (stack.isEmpty()) this.fuelSlot.setItem(0, ItemStack.EMPTY);
            else this.fuelSlot.setItem(0, stack);

            FuelService.inject(level, colonyId, burnTicks);

            lastConsumeServerTick = now;
            syncFuelToDataSlotsIfServer();
        } finally {
            handlingFuelSlot = false;
        }
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

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (this.playerInv.player.level() instanceof ServerLevel level) {
            colonyIdSlot.set(colonyId);

            tryConsumeFuelOnePerTick(level);

            syncFuelToDataSlotsIfServer();
            syncHeatBonusIfServer();
        }
    }

    public int getDisplayFuelMinutes() {
        return fuelMinutes.get();
    }

    public int getDisplayFuelSeconds() {
        return fuelSeconds.get();
    }

    public int getColonyId() {
        return colonyIdSlot.get();
    }

    public double getHeatBonus() {
        return heatBonusScaled.get() / (double) BONUS_SCALE;
    }

    public BlockPos getCorePos() {
        return corePos;
    }
}
