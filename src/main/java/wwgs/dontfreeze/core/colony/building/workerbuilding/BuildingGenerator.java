package wwgs.dontfreeze.core.colony.building.workerbuilding;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wwgs.dontfreeze.api.temperature.fuel.IFuel;
import wwgs.dontfreeze.core.temperature.fuel.FuelSavedData;
import wwgs.dontfreeze.core.temperature.fuel.VanillaBurnTimeFuel;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class BuildingGenerator extends AbstractBuilding
{
    public static final String GENERATOR = "generator";
    private static final int INJECT_THRESHOLD_TICKS = 20 * 60 * 5;
    private static final long REQUEST_COOLDOWN_MS = 60_000;

    private static final IFuel FUEL_RULE = new VanillaBurnTimeFuel();

    @Nullable
    private IToken<?> activeFuelRequest;

    private long lastRequestMs = 0;

    public BuildingGenerator(@NotNull IColony colony, BlockPos pos)
    {
        super(colony, pos);
    }

    @NotNull
    @Override
    public String getSchematicName()
    {
        return GENERATOR;
    }

    @Override
    public void onColonyTick(IColony colony)
    {
        super.onColonyTick(colony);

        if (!(colony.getWorld() instanceof ServerLevel level))
        {
            return;
        }

        final int colonyId = colony.getID();
        final int curTicks = getStoredFuel(level, colonyId);

        if (curTicks > INJECT_THRESHOLD_TICKS)
        {
            return;
        }

        boolean injected = tryInjectOneCoal(level, colonyId);
        if (!injected)
        {
            ensureCoalRequest();
        }
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

    private boolean tryInjectOneCoal(ServerLevel level, int colonyId)
    {
        if (tryInjectFromBuildingInventory(level, colonyId))
        {
            return true;
        }

        var handlers = this.getHandlers();
        if (handlers != null && !handlers.isEmpty())
        {
            if (tryInjectFromHandlers(level, colonyId, handlers))
            {
                return true;
            }
        }

        var te = this.getTileEntity();
        if (te == null)
        {
            return false;
        }

        var pos = te.getTilePos();
        if (!level.isLoaded(pos))
        {
            return false;
        }

        for (Direction dir : Direction.values())
        {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
            if (handler != null && tryInjectFromOneHandler(level, colonyId, handler))
            {
                return true;
            }
        }

        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        return handler != null && tryInjectFromOneHandler(level, colonyId, handler);
    }

    private boolean tryInjectFromHandlers(ServerLevel level, int colonyId, List<IItemHandler> handlers)
    {
        for (IItemHandler handler : handlers)
        {
            if (handler != null && tryInjectFromOneHandler(level, colonyId, handler))
            {
                return true;
            }
        }
        return false;
    }

    private boolean tryInjectFromOneHandler(ServerLevel level, int colonyId, IItemHandler handler)
    {
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || !stack.is(Items.COAL))
            {
                continue;
            }

            ItemStack extracted = handler.extractItem(slot, 1, false);

            if (extracted.isEmpty() && handler instanceof IItemHandlerModifiable modifiable)
            {
                ItemStack in = modifiable.getStackInSlot(slot);
                if (!in.isEmpty() && in.is(Items.COAL))
                {
                    extracted = new ItemStack(Items.COAL, 1);
                    int remain = in.getCount() - 1;

                    if (remain <= 0)
                    {
                        modifiable.setStackInSlot(slot, ItemStack.EMPTY);
                    }
                    else
                    {
                        ItemStack newStack = in.copy();
                        newStack.setCount(remain);
                        modifiable.setStackInSlot(slot, newStack);
                    }
                }
            }

            if (extracted.isEmpty())
            {
                continue;
            }

            int burnTicks = getBurnTicks(extracted);
            if (burnTicks <= 0)
            {
                continue;
            }

            injectFuel(level, colonyId, burnTicks);
            return true;
        }
        return false;
    }

    private boolean tryInjectFromBuildingInventory(ServerLevel level, int colonyId)
    {
        Object inv = tryGetBuildingInventoryObject();
        return switch (inv)
        {
            case IItemHandler handler -> tryInjectFromOneHandler(level, colonyId, handler);
            case Container container -> tryInjectFromContainer(level, colonyId, container);
            case null, default -> false;
        };
    }

    public int getTotalFuelTicksInBuildingInventory()
    {
        Object inv = tryGetBuildingInventoryObject();
        if (inv == null)
        {
            return 0;
        }

        long total = 0;

        if (inv instanceof IItemHandler handler)
        {
            for (int i = 0; i < handler.getSlots(); i++)
            {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty())
                {
                    continue;
                }

                int burn = getBurnTicks(stack);
                if (burn <= 0)
                {
                    continue;
                }

                total += (long) burn * stack.getCount();
            }
            return (int) Math.min(Integer.MAX_VALUE, total);
        }

        if (inv instanceof Container container)
        {
            for (int i = 0; i < container.getContainerSize(); i++)
            {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty())
                {
                    continue;
                }

                int burn = getBurnTicks(stack);
                if (burn <= 0)
                {
                    continue;
                }

                total += (long) burn * stack.getCount();
            }
            return (int) Math.min(Integer.MAX_VALUE, total);
        }

        return 0;
    }

    public int getTotalFuelTicksIncludingHandlers()
    {
        long total = getTotalFuelTicksInBuildingInventory();

        try
        {
            for (IItemHandler handler : this.getHandlers())
            {
                if (handler == null)
                {
                    continue;
                }

                for (int slot = 0; slot < handler.getSlots(); slot++)
                {
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (stack.isEmpty())
                    {
                        continue;
                    }

                    int burn = getBurnTicks(stack);
                    if (burn <= 0)
                    {
                        continue;
                    }

                    total += (long) burn * stack.getCount();
                }
            }
        }
        catch (Throwable ignored)
        {
        }

        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private boolean tryInjectFromContainer(ServerLevel level, int colonyId, Container container)
    {
        for (int i = 0; i < container.getContainerSize(); i++)
        {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || !stack.is(Items.COAL))
            {
                continue;
            }

            ItemStack removed = container.removeItem(i, 1);
            if (removed.isEmpty())
            {
                ItemStack cur = container.getItem(i);
                if (!cur.isEmpty() && cur.is(Items.COAL) && cur.getCount() > 0)
                {
                    removed = new ItemStack(Items.COAL, 1);
                    ItemStack newStack = cur.copy();
                    newStack.setCount(cur.getCount() - 1);
                    container.setItem(i, newStack.getCount() <= 0 ? ItemStack.EMPTY : newStack);
                }
            }

            if (removed.isEmpty())
            {
                continue;
            }

            int burnTicks = getBurnTicks(removed);
            if (burnTicks <= 0)
            {
                continue;
            }

            injectFuel(level, colonyId, burnTicks);
            return true;
        }

        return false;
    }

    @Nullable
    private Object tryGetBuildingInventoryObject()
    {
        final String[] candidates = new String[] { "getInventory", "getBuildingInventory", "getContainer", "getStorage" };

        for (String name : candidates)
        {
            Object result = invokeNoArg(this, name);
            if (result != null)
            {
                return result;
            }
        }

        var te = this.getTileEntity();
        if (te != null)
        {
            Object inv = invokeNoArg(te, "getInventory");
            if (inv != null)
            {
                return inv;
            }

            return invokeNoArg(te, "getContainer");
        }

        return null;
    }

    @Nullable
    private static Object invokeNoArg(Object target, String methodName)
    {
        try
        {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        }
        catch (Throwable ignored)
        {
            return null;
        }
    }

    @Override
    public int buildingRequiresCertainAmountOfItem(ItemStack stack,
                                                   List<ItemStorage> localAlreadyKept,
                                                   boolean inventory,
                                                   JobEntry jobEntry)
    {
        if (stack != null && stack.is(Items.COAL))
        {
            return 0;
        }
        return super.buildingRequiresCertainAmountOfItem(stack, localAlreadyKept, inventory, jobEntry);
    }

    @Override
    public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount()
    {
        return Map.of();
    }

    private int getDesiredCoalStockCountByLevel()
    {
        int level = this.getBuildingLevel();
        return switch (level)
        {
            case 1 -> 16;
            case 2 -> 32;
            case 3 -> 48;
            default -> 64;
        };
    }

    private void ensureCoalRequest()
    {
        if (isFuelRequestOpen())
        {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastRequestMs < REQUEST_COOLDOWN_MS)
        {
            return;
        }

        ItemStack coal = new ItemStack(Items.COAL);
        int desired = getDesiredCoalStockCountByLevel();
        int min = Math.max(1, desired / 2);

        Stack stackReq = new Stack(coal, desired, min);
        stackReq.setCanBeResolvedByBuilding(false);

        IToken<?> token = this.createRequest(stackReq, false);
        if (token != null)
        {
            activeFuelRequest = token;
            lastRequestMs = now;
            markDirty();
        }
    }

    private boolean isFuelRequestOpen()
    {
        if (activeFuelRequest == null)
        {
            return false;
        }

        for (var tokens : getOpenRequestsByRequestableType().values())
        {
            if (tokens.contains(activeFuelRequest))
            {
                return true;
            }
        }

        activeFuelRequest = null;
        return false;
    }
}