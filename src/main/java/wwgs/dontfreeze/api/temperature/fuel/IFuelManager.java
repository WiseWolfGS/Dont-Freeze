package wwgs.dontfreeze.api.temperature.fuel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IFuelManager
{
    @Nullable
    IFuelData getFuelData(@NotNull ServerLevel level, int colonyId);

    @NotNull
    IFuelData getOrCreateFuelData(@NotNull ServerLevel level, int colonyId);

    boolean isFuel(@NotNull ItemStack stack);

    int getFuelValue(@NotNull ItemStack stack);

    int insertFuel(@NotNull ServerLevel level, int colonyId, @NotNull ItemStack stack);

    int addFuel(@NotNull ServerLevel level, int colonyId, int amount);

    int consumeFuel(@NotNull ServerLevel level, int colonyId, int amount);

    int getFuelCostPerSecond(@NotNull ServerLevel level, int colonyId, double heatBonus);

    default int getStoredFuel(@NotNull ServerLevel level, int colonyId)
    {
        IFuelData data = getFuelData(level, colonyId);
        return data == null ? 0 : data.getStored();
    }

    default boolean hasFuel(@NotNull ServerLevel level, int colonyId)
    {
        return getStoredFuel(level, colonyId) > 0;
    }
}