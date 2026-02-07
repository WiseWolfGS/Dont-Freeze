package net.WWGS.dontfreeze.core.colony.building.workerbuilding;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.WWGS.dontfreeze.core.colony.fuel.platform.FuelSavedData;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelBurnTime;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class BuildingGenerator extends AbstractBuilding
{
    public static final String GENERATOR = "generator";

    /** If colony fuel is below this, the building will try to inject coal and/or request coal. */
    private static final int INJECT_THRESHOLD_TICKS = 20 * 60 * 5; // 5 min

    /** Prevent request spam if something goes wrong with delivery/injection. */
    private static final long REQUEST_COOLDOWN_MS = 60_000;

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
        if (!(colony.getWorld() instanceof ServerLevel level)) return;

        final int colonyId = colony.getID();
        final int curTicks = FuelSavedData.get(level).getFuel(colonyId);

        if (curTicks > INJECT_THRESHOLD_TICKS) return;

        boolean injected = tryInjectOneCoal(level, colonyId);
        if (!injected)
        {
            ensureCoalRequest();
        }
    }

    // -----------------------------------------
    // Fuel injection (coal only)
    // -----------------------------------------

    /**
     * Injection priority:
     *  1) "Building inventory" (the same inventory opened by the chest button in the hut UI)
     *  2) Building handlers (getHandlers)
     *  3) Hut block item handler capability (all directions + null)
     */
    private boolean tryInjectOneCoal(ServerLevel level, int colonyId)
    {
        // ✅ 1) IMPORTANT: Try the building inventory ("보관함") FIRST.
        // This is where MineColonies counts items to fulfill requests.
        if (tryInjectFromBuildingInventory(level, colonyId)) return true;

        // 2) Handlers provided by the building (racks, modules, etc.)
        var handlers = this.getHandlers();
        if (handlers != null && !handlers.isEmpty())
        {
            if (tryInjectFromHandlers(level, colonyId, handlers)) return true;
        }

        // 3) Capability on the hut block entity (sometimes insert-only wrappers exist)
        var te = this.getTileEntity();
        if (te == null) return false;

        var pos = te.getTilePos();
        if (!level.hasChunkAt(pos)) return false;

        for (Direction dir : Direction.values())
        {
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
            if (h != null && tryInjectFromOneHandler(level, colonyId, h)) return true;
        }

        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        return h != null && tryInjectFromOneHandler(level, colonyId, h);
    }

    private boolean tryInjectFromHandlers(ServerLevel level, int colonyId, List<IItemHandler> handlers)
    {
        for (var h : handlers)
        {
            if (h == null) continue;
            if (tryInjectFromOneHandler(level, colonyId, h)) return true;
        }
        return false;
    }

    /**
     * Normal IItemHandler extraction. Includes a fallback for IItemHandlerModifiable.
     */
    private boolean tryInjectFromOneHandler(ServerLevel level, int colonyId, IItemHandler handler)
    {
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (!stack.is(Items.COAL)) continue;

            // 1) normal extract
            ItemStack extracted = handler.extractItem(slot, 1, false);

            // 2) fallback: modifiable direct slot decrement
            if (extracted.isEmpty() && handler instanceof IItemHandlerModifiable mod)
            {
                ItemStack in = mod.getStackInSlot(slot);
                if (!in.isEmpty() && in.is(Items.COAL))
                {
                    extracted = new ItemStack(Items.COAL, 1);
                    int remain = in.getCount() - 1;

                    if (remain <= 0)
                    {
                        mod.setStackInSlot(slot, ItemStack.EMPTY);
                    }
                    else
                    {
                        ItemStack newStack = in.copy();
                        newStack.setCount(remain);
                        mod.setStackInSlot(slot, newStack);
                    }
                }
            }

            if (extracted.isEmpty()) continue;

            final int burnTicks = FuelBurnTime.getBurnTicks(extracted);
            if (burnTicks <= 0) continue;

            FuelService.inject(level, colonyId, burnTicks);
            return true;
        }
        return false;
    }

    // -----------------------------------------
    // ✅ Building inventory ("보관함") extraction
    // -----------------------------------------

    /**
     * The hut UI "보관함" button typically opens a building-owned inventory/container.
     * MineColonies uses this inventory to satisfy requests, but it may not be exposed via
     * block capabilities/handlers in a way that allows extraction.
     *
     * To be robust across MineColonies versions, we use reflection to find one of the common
     * inventory getters and then remove one coal directly.
     */
    private boolean tryInjectFromBuildingInventory(ServerLevel level, int colonyId)
    {
        Object inv = tryGetBuildingInventoryObject();
        if (inv == null) return false;

        // Case A: It's already an IItemHandler
        if (inv instanceof IItemHandler handler)
        {
            return tryInjectFromOneHandler(level, colonyId, handler);
        }

        // Case B: It's a vanilla Container (or something implementing it)
        if (inv instanceof Container container)
        {
            return tryInjectFromContainer(level, colonyId, container);
        }

        return false;
    }

    private boolean tryInjectFromContainer(ServerLevel level, int colonyId, Container container)
    {
        for (int i = 0; i < container.getContainerSize(); i++)
        {
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) continue;
            if (!s.is(Items.COAL)) continue;

            // remove 1 item from the container
            ItemStack removed = container.removeItem(i, 1);
            if (removed.isEmpty())
            {
                // Some containers might not support removeItem properly; fallback: manual decrement if possible.
                ItemStack cur = container.getItem(i);
                if (!cur.isEmpty() && cur.is(Items.COAL) && cur.getCount() > 0)
                {
                    removed = new ItemStack(Items.COAL, 1);
                    ItemStack newStack = cur.copy();
                    newStack.setCount(cur.getCount() - 1);
                    container.setItem(i, newStack.getCount() <= 0 ? ItemStack.EMPTY : newStack);
                }
            }

            if (removed.isEmpty()) continue;

            int burnTicks = FuelBurnTime.getBurnTicks(removed);
            if (burnTicks <= 0) continue;

            FuelService.inject(level, colonyId, burnTicks);
            return true;
        }
        return false;
    }

    /**
     * Try multiple method names that MineColonies uses across versions to expose building inventory.
     * We call them on 'this' (AbstractBuilding instance).
     */
    @Nullable
    private Object tryGetBuildingInventoryObject()
    {
        // Common candidates across MineColonies versions:
        // - getInventory()
        // - getBuildingInventory()
        // - getContainer()
        // - getStorage()
        // - getTileEntity().getInventory() (sometimes)
        final String[] candidates = new String[]
                {
                        "getInventory",
                        "getBuildingInventory",
                        "getContainer",
                        "getStorage"
                };

        for (String name : candidates)
        {
            Object r = invokeNoArg(this, name);
            if (r != null) return r;
        }

        // As a last resort, try tile entity method names
        var te = this.getTileEntity();
        if (te != null)
        {
            Object t = invokeNoArg(te, "getInventory");
            if (t != null) return t;

            Object t2 = invokeNoArg(te, "getContainer");
            if (t2 != null) return t2;
        }

        return null;
    }

    @Nullable
    private static Object invokeNoArg(Object target, String methodName)
    {
        try
        {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(target);
        }
        catch (Throwable ignored)
        {
            return null;
        }
    }

    // -----------------------------------------
    // MineColonies request system (clipboard)
    // -----------------------------------------

    /**
     * Disable MineColonies' default auto-requesting for coal.
     * We create requests explicitly via ensureCoalRequest().
     */
    @Override
    public int buildingRequiresCertainAmountOfItem(ItemStack stack,
                                                   List<ItemStorage> localAlreadyKept,
                                                   boolean inventory,
                                                   JobEntry jobEntry)
    {
        if (stack != null && stack.is(Items.COAL))
        {
            return 0; // block auto request for coal
        }
        return super.buildingRequiresCertainAmountOfItem(stack, localAlreadyKept, inventory, jobEntry);
    }

    @Override
    public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount()
    {
        return Map.of(); // block auto requests entirely
    }

    private int getDesiredCoalStockCountByLevel()
    {
        int lvl = this.getBuildingLevel();
        return switch (lvl)
        {
            case 1 -> 16;
            case 2 -> 32;
            case 3 -> 48;
            default -> 64;
        };
    }

    private void ensureCoalRequest()
    {
        if (isFuelRequestOpen()) return;

        long now = System.currentTimeMillis();
        if (now - lastRequestMs < REQUEST_COOLDOWN_MS) return;

        ItemStack coal = new ItemStack(Items.COAL);
        int desired = getDesiredCoalStockCountByLevel();
        int min = Math.max(1, desired / 2);

        var stackReq = new Stack(coal, desired, min);
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
        if (activeFuelRequest == null) return false;

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
