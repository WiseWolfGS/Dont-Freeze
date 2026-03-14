package wwgs.dontfreeze.core.temperature.fuel.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import wwgs.dontfreeze.api.temperature.fuel.IFuel;
import wwgs.dontfreeze.api.util.QueryUtils;
import wwgs.dontfreeze.core.common.menu.DFMenus;
import wwgs.dontfreeze.core.temperature.fuel.FuelCostCalculator;
import wwgs.dontfreeze.core.temperature.fuel.FuelSavedData;
import wwgs.dontfreeze.core.temperature.fuel.VanillaBurnTimeFuel;
import wwgs.dontfreeze.core.temperature.heat.HeatBonusSavedData;

public class MenuGeneratorCore extends AbstractContainerMenu
{
    private static final int FUEL_SLOT_X = 15;
    private static final int FUEL_SLOT_Y = 123;

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int BONUS_SCALE = 100;
    private static final IFuel FUEL_RULE = new VanillaBurnTimeFuel();

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
    private final DataSlot buildingCount = DataSlot.standalone();
    private final DataSlot buildingLevelSum = DataSlot.standalone();
    private final DataSlot costPerSecond = DataSlot.standalone();
    private final DataSlot coalMinutes = DataSlot.standalone();
    private final DataSlot coalSeconds = DataSlot.standalone();

    public MenuGeneratorCore(int containerId, Inventory inv, FriendlyByteBuf buf)
    {
        this(containerId, inv, buf.readBlockPos());
    }

    public MenuGeneratorCore(int containerId, Inventory inv, BlockPos corePos)
    {
        super(DFMenus.MENU_GENERATOR_CORE.get(), containerId);
        this.playerInv = inv;
        this.corePos = corePos;

        this.addDataSlot(colonyIdSlot);
        this.addDataSlot(fuelMinutes);
        this.addDataSlot(fuelSeconds);
        this.addDataSlot(heatBonusScaled);
        this.addDataSlot(buildingCount);
        this.addDataSlot(buildingLevelSum);
        this.addDataSlot(costPerSecond);
        this.addDataSlot(coalMinutes);
        this.addDataSlot(coalSeconds);

        this.fuelSlot = new SimpleContainer(1)
        {
            @Override
            public void setChanged()
            {
                super.setChanged();
                MenuGeneratorCore.this.onFuelSlotChanged();
            }
        };

        this.addSlot(new Slot(this.fuelSlot, 0, FUEL_SLOT_X, FUEL_SLOT_Y));
        addPlayerInventorySlots(inv, 15, 155);
        addHotbarSlots(inv, 15, 213);

        computeColonyIdIfServer();
        syncHeatBonusIfServer();
        syncFuelToDataSlotsIfServer();
    }

    private void onFuelSlotChanged()
    {
    }

    private static int getStoredFuel(@NotNull ServerLevel level, int colonyId)
    {
        return FuelSavedData.get(level).getOrCreate(colonyId).getStored();
    }

    private static void injectFuel(@NotNull ServerLevel level, int colonyId, int burnTicks)
    {
        if (burnTicks <= 0)
        {
            return;
        }

        FuelSavedData data = FuelSavedData.get(level);
        data.getOrCreate(colonyId).addFuel(burnTicks);
        data.setDirty();
    }

    private static int getBurnTicks(@NotNull ItemStack stack)
    {
        return FUEL_RULE.getFuelValue(stack);
    }

    private void addPlayerInventorySlots(Inventory inv, int left, int top)
    {
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(inv, 9 + row * 9 + col, left + col * 18, top + row * 18));
            }
        }
    }

    private void addHotbarSlots(Inventory inv, int left, int top)
    {
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(inv, col, left + col * 18, top));
        }
    }

    private void computeColonyIdIfServer()
    {
        Player player = this.playerInv.player;
        if (!(player.level() instanceof ServerLevel level))
        {
            return;
        }

        var query = QueryUtils.buildingQuery();
        if (query == null)
        {
            colonyId = -1;
            colonyIdSlot.set(-1);
            return;
        }

        var townHall = query.findNearestTownHallPos(level, corePos);
        colonyId = townHall == null ? -1 : townHall.getColonyId();
        colonyIdSlot.set(colonyId);
    }

    private void syncFuelToDataSlotsIfServer()
    {
        Player player = this.playerInv.player;
        if (!(player.level() instanceof ServerLevel level))
        {
            return;
        }

        int fuel = colonyId < 0 ? 0 : getStoredFuel(level, colonyId);
        double bonus = getHeatBonus();
        int cps = colonyId < 0 ? 0 : Math.max(1, FuelCostCalculator.compute(level, colonyId, bonus));

        buildingCount.set(0);
        buildingLevelSum.set(0);
        costPerSecond.set(cps);

        if (fuel <= 0 || cps <= 0)
        {
            fuelMinutes.set(0);
            fuelSeconds.set(0);
            coalMinutes.set(0);
            coalSeconds.set(0);
            return;
        }

        int totalSeconds = fuel / cps;
        fuelMinutes.set(totalSeconds / SECONDS_PER_MINUTE);
        fuelSeconds.set(totalSeconds % SECONDS_PER_MINUTE);

        syncCoalEfficiencyIfServer(cps);
    }

    private void syncCoalEfficiencyIfServer(int costPerSecond)
    {
        if (costPerSecond <= 0)
        {
            coalMinutes.set(0);
            coalSeconds.set(0);
            return;
        }

        int burnTicks = getBurnTicks(new ItemStack(Items.COAL));
        if (burnTicks <= 0)
        {
            coalMinutes.set(0);
            coalSeconds.set(0);
            return;
        }

        int totalSeconds = burnTicks / costPerSecond;
        coalMinutes.set(totalSeconds / SECONDS_PER_MINUTE);
        coalSeconds.set(totalSeconds % SECONDS_PER_MINUTE);
    }

    private void syncHeatBonusIfServer()
    {
        Player player = this.playerInv.player;
        if (!(player.level() instanceof ServerLevel level))
        {
            return;
        }

        if (colonyId < 0)
        {
            heatBonusScaled.set(0);
            return;
        }

        double bonus = HeatBonusSavedData.get(level).getBonus(colonyId);
        heatBonusScaled.set((int) Math.round(bonus * BONUS_SCALE));
    }

    private void tryConsumeFuelOnePerTick(ServerLevel level)
    {
        if (colonyId < 0)
        {
            return;
        }

        long now = level.getGameTime();
        if (now == lastConsumeServerTick || handlingFuelSlot)
        {
            return;
        }

        ItemStack stack = this.fuelSlot.getItem(0);
        if (stack.isEmpty())
        {
            return;
        }

        int burnTicks = getBurnTicks(stack);
        if (burnTicks <= 0)
        {
            return;
        }

        handlingFuelSlot = true;
        try
        {
            stack.shrink(1);
            if (stack.isEmpty())
            {
                this.fuelSlot.setItem(0, ItemStack.EMPTY);
            }
            else
            {
                this.fuelSlot.setItem(0, stack);
            }

            injectFuel(level, colonyId, burnTicks);
            lastConsumeServerTick = now;
            syncFuelToDataSlotsIfServer();
        }
        finally
        {
            handlingFuelSlot = false;
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index)
    {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem())
        {
            return empty;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int fuelSlotIndex = 0;
        int playerInvStart = 1;
        int playerInvEnd = this.slots.size();

        if (index == fuelSlotIndex)
        {
            if (!this.moveItemStackTo(stack, playerInvStart, playerInvEnd, true))
            {
                return empty;
            }
        }
        else
        {
            if (!this.moveItemStackTo(stack, fuelSlotIndex, fuelSlotIndex + 1, false))
            {
                return empty;
            }
        }

        if (stack.isEmpty())
        {
            slot.set(ItemStack.EMPTY);
        }
        else
        {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return true;
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        if (this.playerInv.player.level() instanceof ServerLevel level)
        {
            colonyIdSlot.set(colonyId);
            tryConsumeFuelOnePerTick(level);
            syncHeatBonusIfServer();
            syncFuelToDataSlotsIfServer();
        }
    }

    public int getDisplayFuelMinutes()
    {
        return fuelMinutes.get();
    }

    public int getDisplayFuelSeconds()
    {
        return fuelSeconds.get();
    }

    public int getColonyId()
    {
        return colonyIdSlot.get();
    }

    public double getHeatBonus()
    {
        return heatBonusScaled.get() / (double) BONUS_SCALE;
    }

    public int getBuildingCount()
    {
        return buildingCount.get();
    }

    public int getBuildingLevelSum()
    {
        return buildingLevelSum.get();
    }

    public int getCostPerSecond()
    {
        return costPerSecond.get();
    }

    public int getCoalEfficiencyMinutes()
    {
        return coalMinutes.get();
    }

    public int getCoalEfficiencySeconds()
    {
        return coalSeconds.get();
    }

    public BlockPos getCorePos()
    {
        return corePos;
    }
}