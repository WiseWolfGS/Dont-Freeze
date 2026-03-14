package wwgs.dontfreeze.core.temperature.fuel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wwgs.dontfreeze.api.temperature.fuel.IFuel;
import wwgs.dontfreeze.api.temperature.fuel.IFuelData;
import wwgs.dontfreeze.api.temperature.fuel.IFuelManager;

public class FuelManager implements IFuelManager
{
    private final IFuel fuelRule;

    public FuelManager(IFuel fuelRule)
    {
        this.fuelRule = fuelRule;
    }

    @Override
    public @Nullable IFuelData getFuelData(@NotNull ServerLevel level, int colonyId)
    {
        return FuelSavedData.get(level).get(colonyId);
    }

    @Override
    public @NotNull IFuelData getOrCreateFuelData(@NotNull ServerLevel level, int colonyId)
    {
        return FuelSavedData.get(level).getOrCreate(colonyId);
    }

    @Override
    public boolean isFuel(@NotNull ItemStack stack)
    {
        return fuelRule.isValid(stack);
    }

    @Override
    public int getFuelValue(@NotNull ItemStack stack)
    {
        return fuelRule.getFuelValue(stack);
    }

    @Override
    public int insertFuel(@NotNull ServerLevel level, int colonyId, @NotNull ItemStack stack)
    {
        if (!isFuel(stack))
        {
            return 0;
        }

        int value = getFuelValue(stack);
        return addFuel(level, colonyId, value);
    }

    @Override
    public int addFuel(@NotNull ServerLevel level, int colonyId, int amount)
    {
        return getOrCreateFuelData(level, colonyId).addFuel(amount);
    }

    @Override
    public int consumeFuel(@NotNull ServerLevel level, int colonyId, int amount)
    {
        return getOrCreateFuelData(level, colonyId).consumeFuel(amount);
    }

    @Override
    public int getFuelCostPerSecond(@NotNull ServerLevel level, int colonyId, double heatBonus)
    {
        return FuelCostCalculator.compute(level, colonyId, heatBonus);
    }
}
