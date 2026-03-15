package wwgs.dontfreeze.core.colony.building.workerbuilding;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.managers.interfaces.IStatisticsManager;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingStatisticsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.api.temperature.fuel.IFuel;
import wwgs.dontfreeze.core.temperature.fuel.FuelSavedData;
import wwgs.dontfreeze.core.temperature.fuel.VanillaBurnTimeFuel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class BuildingGenerator extends AbstractBuilding
{
    public static final String GENERATOR = "generator";

    private static final int INJECT_THRESHOLD_TICKS = 20 * 60 * 5;
    private static final long REQUEST_COOLDOWN_MS = 24_000L;

    /**
     * Scan radius around the hut / tile entity.
     * Big enough to catch nearby racks in most schematics without scanning the entire colony.
     */
    private static final int DEFAULT_RACK_SCAN_RADIUS_XZ = 12;
    private static final int DEFAULT_RACK_SCAN_RADIUS_Y = 6;

    private static final IFuel FUEL_RULE = new VanillaBurnTimeFuel();
    private static long coalConsumedTotal = 0L;
    private static final String STAT_USED_COAL = "dontfreeze_used_coal";

    @Nullable
    private IToken<?> activeFuelRequest;

    private long lastRequestMs = 0L;

    public BuildingGenerator(@NotNull final IColony colony, final BlockPos pos)
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
    public void onColonyTick(final IColony colony)
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

        final boolean injected = tryInjectOneCoal(level, colonyId);
        if (!injected)
        {
            ensureCoalRequest();
        }
    }

    private static int getStoredFuel(@NotNull final ServerLevel level, final int colonyId)
    {
        return FuelSavedData.get(level).getOrCreate(colonyId).getStored();
    }

    private void injectFuel(@NotNull final ServerLevel level, final int colonyId, final int burnTicks)
    {
        if (burnTicks <= 0)
        {
            return;
        }

        final FuelSavedData data = FuelSavedData.get(level);
        data.getOrCreate(colonyId).addFuel(burnTicks);
        onCoalConsumed();
        data.setDirty();
    }

    private static int getBurnTicks(@NotNull final ItemStack stack)
    {
        return FUEL_RULE.getFuelValue(stack);
    }

    private boolean tryInjectOneCoal(@NotNull final ServerLevel level, final int colonyId)
    {
        if (tryInjectFromBuildingInventory(level, colonyId))
        {
            return true;
        }

        final List<IItemHandler> handlers = safeGetHandlers();
        if (!handlers.isEmpty() && tryInjectFromHandlers(level, colonyId, handlers))
        {
            return true;
        }

        if (tryInjectFromNearbyRacks(level, colonyId))
        {
            return true;
        }

        final Object te = this.getTileEntity();
        if (te == null)
        {
            return false;
        }

        final BlockPos pos = tryGetTilePos(te);
        if (pos == null || !level.isLoaded(pos))
        {
            return false;
        }

        return tryInjectFromBlockCapabilities(level, colonyId, pos);
    }

    private boolean tryInjectFromHandlers(@NotNull final ServerLevel level, final int colonyId, @NotNull final List<IItemHandler> handlers)
    {
        for (final IItemHandler handler : handlers)
        {
            if (handler != null && tryInjectFromOneHandler(level, colonyId, handler))
            {
                return true;
            }
        }
        return false;
    }

    private boolean tryInjectFromOneHandler(@NotNull final ServerLevel level, final int colonyId, @NotNull final IItemHandler handler)
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
                final ItemStack in = modifiable.getStackInSlot(slot);
                if (!in.isEmpty() && in.is(Items.COAL))
                {
                    extracted = new ItemStack(Items.COAL, 1);
                    final int remain = in.getCount() - 1;
                    if (remain <= 0)
                    {
                        modifiable.setStackInSlot(slot, ItemStack.EMPTY);
                    }
                    else
                    {
                        final ItemStack newStack = in.copy();
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

    private boolean tryInjectFromBuildingInventory(@NotNull final ServerLevel level, final int colonyId)
    {
        final Object inv = tryGetBuildingInventoryObject();
        if (inv instanceof IItemHandler handler)
        {
            return tryInjectFromOneHandler(level, colonyId, handler);
        }
        if (inv instanceof Container container)
        {
            return tryInjectFromContainer(level, colonyId, container);
        }
        return false;
    }

    public int getTotalFuelTicksInBuildingInventory()
    {
        final Object inv = tryGetBuildingInventoryObject();
        if (inv == null)
        {
            return 0;
        }

        long total = 0L;
        if (inv instanceof IItemHandler handler)
        {
            for (int i = 0; i < handler.getSlots(); i++)
            {
                final ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty())
                {
                    continue;
                }

                final int burn = getBurnTicks(stack);
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
                final ItemStack stack = container.getItem(i);
                if (stack.isEmpty())
                {
                    continue;
                }

                final int burn = getBurnTicks(stack);
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

        for (final IItemHandler handler : safeGetHandlers())
        {
            if (handler == null)
            {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++)
            {
                final ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty())
                {
                    continue;
                }

                final int burn = getBurnTicks(stack);
                if (burn <= 0)
                {
                    continue;
                }

                total += (long) burn * stack.getCount();
            }
        }

        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private boolean tryInjectFromContainer(@NotNull final ServerLevel level, final int colonyId, @NotNull final Container container)
    {
        for (int i = 0; i < container.getContainerSize(); i++)
        {
            final ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || !stack.is(Items.COAL))
            {
                continue;
            }

            ItemStack removed = container.removeItem(i, 1);
            if (removed.isEmpty())
            {
                final ItemStack cur = container.getItem(i);
                if (!cur.isEmpty() && cur.is(Items.COAL) && cur.getCount() > 0)
                {
                    removed = new ItemStack(Items.COAL, 1);
                    final ItemStack newStack = cur.copy();
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

    private boolean tryInjectFromNearbyRacks(@NotNull final ServerLevel level, final int colonyId)
    {
        final BlockPos center = getSearchCenter();
        final int radiusXZ = getRackScanRadiusXZ();
        final int radiusY = getRackScanRadiusY();

        final List<BlockPos> rackPositions = new ArrayList<>();
        final List<BlockPos> otherInventoryPositions = new ArrayList<>();

        final BlockPos min = center.offset(-radiusXZ, -radiusY, -radiusXZ);
        final BlockPos max = center.offset(radiusXZ, radiusY, radiusXZ);

        for (final BlockPos pos : BlockPos.betweenClosed(min, max))
        {
            if (!level.isLoaded(pos))
            {
                continue;
            }

            final BlockEntity be = level.getBlockEntity(pos);
            final boolean hasInventory = be instanceof Container || hasAnyItemHandler(level, pos);
            if (!hasInventory)
            {
                continue;
            }

            if (isLikelyRack(level, pos, be))
            {
                rackPositions.add(pos.immutable());
            }
            else
            {
                otherInventoryPositions.add(pos.immutable());
            }
        }

        // First prefer actual rack-looking inventories.
        for (final BlockPos pos : rackPositions)
        {
            if (tryInjectFromInventoryAt(level, colonyId, pos))
            {
                return true;
            }
        }

        // Fallback: nearby inventories that MineColonies exposed in a non-rack block entity.
        for (final BlockPos pos : otherInventoryPositions)
        {
            if (tryInjectFromInventoryAt(level, colonyId, pos))
            {
                return true;
            }
        }

        return false;
    }

    private boolean tryInjectFromInventoryAt(@NotNull final ServerLevel level, final int colonyId, @NotNull final BlockPos pos)
    {
        final Set<IItemHandler> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        IItemHandler nullSide = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (nullSide != null)
        {
            visited.add(nullSide);
            if (tryInjectFromOneHandler(level, colonyId, nullSide))
            {
                return true;
            }
        }

        for (final Direction dir : Direction.values())
        {
            final IItemHandler sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
            if (sided == null || !visited.add(sided))
            {
                continue;
            }

            if (tryInjectFromOneHandler(level, colonyId, sided))
            {
                return true;
            }
        }

        final BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container container)
        {
            return tryInjectFromContainer(level, colonyId, container);
        }

        final Object inventory = tryGetInventoryObject(be);
        if (inventory instanceof Container container)
        {
            return tryInjectFromContainer(level, colonyId, container);
        }
        if (inventory instanceof IItemHandler handler)
        {
            return tryInjectFromOneHandler(level, colonyId, handler);
        }

        return false;
    }

    private boolean tryInjectFromBlockCapabilities(@NotNull final ServerLevel level, final int colonyId, @NotNull final BlockPos pos)
    {
        final Set<IItemHandler> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        for (final Direction dir : Direction.values())
        {
            final IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
            if (handler != null && visited.add(handler) && tryInjectFromOneHandler(level, colonyId, handler))
            {
                return true;
            }
        }

        final IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        return handler != null && visited.add(handler) && tryInjectFromOneHandler(level, colonyId, handler);
    }

    private boolean hasAnyItemHandler(@NotNull final ServerLevel level, @NotNull final BlockPos pos)
    {
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null)
        {
            return true;
        }

        for (final Direction dir : Direction.values())
        {
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir) != null)
            {
                return true;
            }
        }

        return false;
    }

    private int getRackScanRadiusXZ()
    {
        final int level = Math.max(1, this.getBuildingLevel());
        return DEFAULT_RACK_SCAN_RADIUS_XZ + Math.min(8, (level - 1) * 2);
    }

    private int getRackScanRadiusY()
    {
        return DEFAULT_RACK_SCAN_RADIUS_Y;
    }

    @NotNull
    private BlockPos getSearchCenter()
    {
        final Object te = this.getTileEntity();
        final BlockPos tePos = tryGetTilePos(te);
        if (tePos != null)
        {
            return tePos;
        }
        return this.getID();
    }

    private boolean isLikelyRack(@NotNull final ServerLevel level, @NotNull final BlockPos pos, @Nullable final BlockEntity be)
    {
        final String blockPath = safeLowerCase(String.valueOf(level.getBlockState(pos).getBlock()));
        if (blockPath.contains("rack"))
        {
            return true;
        }

        final String beName = be == null ? "" : safeLowerCase(be.getClass().getName());
        if (beName.contains("rack"))
        {
            return true;
        }

        final String blockEntityTypeName = be == null || be.getType() == null ? "" : safeLowerCase(String.valueOf(be.getType()));
        return blockEntityTypeName.contains("rack");
    }

    @Nullable
    private Object tryGetBuildingInventoryObject()
    {
        final String[] candidates = new String[]{"getInventory", "getBuildingInventory", "getContainer", "getStorage"};
        for (final String name : candidates)
        {
            final Object result = invokeNoArg(this, name);
            if (result != null)
            {
                return result;
            }
        }

        final Object te = this.getTileEntity();
        final Object inv = tryGetInventoryObject(te);
        if (inv != null)
        {
            return inv;
        }

        return null;
    }

    @Nullable
    private Object tryGetInventoryObject(@Nullable final Object target)
    {
        if (target == null)
        {
            return null;
        }

        final String[] names = new String[]{"getInventory", "getBuildingInventory", "getContainer", "getStorage"};
        for (final String name : names)
        {
            final Object result = invokeNoArg(target, name);
            if (result != null)
            {
                return result;
            }
        }

        return null;
    }

    @Nullable
    private BlockPos tryGetTilePos(@Nullable final Object target)
    {
        if (target == null)
        {
            return null;
        }

        try
        {
            final Method method = target.getClass().getMethod("getTilePos");
            method.setAccessible(true);
            final Object value = method.invoke(target);
            if (value instanceof BlockPos pos)
            {
                return pos;
            }
        }
        catch (final Throwable ignored)
        {
        }

        if (target instanceof BlockEntity be)
        {
            return be.getBlockPos();
        }

        return null;
    }

    @Nullable
    private static Object invokeNoArg(@Nullable final Object target, @NotNull final String methodName)
    {
        if (target == null)
        {
            return null;
        }

        try
        {
            final Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        }
        catch (final Throwable ignored)
        {
            return null;
        }
    }

    @NotNull
    private List<IItemHandler> safeGetHandlers()
    {
        try
        {
            final List<IItemHandler> handlers = this.getHandlers();
            return handlers == null ? List.of() : handlers;
        }
        catch (final Throwable ignored)
        {
            return List.of();
        }
    }

    @NotNull
    private static String safeLowerCase(@Nullable final String value)
    {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public int buildingRequiresCertainAmountOfItem(final ItemStack stack,
                                                   final List<ItemStorage> localAlreadyKept,
                                                   final boolean inventory,
                                                   final JobEntry jobEntry)
    {
        if (stack != null && stack.is(Items.COAL))
        {
            return getDesiredCoalStockCountByLevel();
        }
        return super.buildingRequiresCertainAmountOfItem(stack, localAlreadyKept, inventory, jobEntry);
    }

    @Override
    public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount()
    {
        return Map.of(stack -> stack != null && stack.is(Items.COAL), new Tuple<>(getDesiredCoalStockCountByLevel(), Boolean.TRUE));
    }

    private int getDesiredCoalStockCountByLevel()
    {
        final int level = this.getBuildingLevel();
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

        final long now = System.currentTimeMillis();
        if (now - lastRequestMs < REQUEST_COOLDOWN_MS)
        {
            return;
        }

        final ItemStack coal = new ItemStack(Items.COAL);
        final int desired = getDesiredCoalStockCountByLevel();
        final int min = Math.max(1, desired / 2);

        final Stack stackReq = new Stack(coal, desired, min);
        // Allow local building / rack storage resolution as well.
        stackReq.setCanBeResolvedByBuilding(true);

        final IToken<?> token = this.createRequest(stackReq, false);
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

        for (final var tokens : getOpenRequestsByRequestableType().values())
        {
            if (tokens.contains(activeFuelRequest))
            {
                return true;
            }
        }

        activeFuelRequest = null;
        return false;
    }

    public long getCoalConsumedTotal()
    {
        return coalConsumedTotal;
    }

    private void onCoalConsumed()
    {
        coalConsumedTotal++;

        Dontfreeze.LOGGER.info("used_coal ++, total={}, day={}", coalConsumedTotal, this.getColony().getDay());

        final BuildingStatisticsModule statsModule = this.getFirstModuleOccurance(BuildingStatisticsModule.class);
        if (statsModule != null)
        {
            Dontfreeze.LOGGER.info("stats total now={}", statsModule.getBuildingStatisticsManager().getStatTotal(STAT_USED_COAL));
            final int day = this.getColony().getDay();
            statsModule.getBuildingStatisticsManager().incrementBy(STAT_USED_COAL, 1, day);
            statsModule.markDirty();
        }

        this.markDirty();
    }
}
