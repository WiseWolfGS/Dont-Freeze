package wwgs.dontfreeze.core.temperature.fuel;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class FuelSavedData extends SavedData
{
    private static final String DATA_NAME = "dontfreeze_fuel";

    private final Map<Integer, FuelData> fuelData = new HashMap<>();
    private static final Factory<FuelSavedData> FACTORY = new Factory<>(FuelSavedData::new, FuelSavedData::load, null);

    public static FuelSavedData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public FuelSavedData() {}

    public static FuelSavedData load(CompoundTag tag, HolderLookup.Provider provider)
    {
        FuelSavedData data = new FuelSavedData();

        CompoundTag mapTag = tag.getCompound("fuel");

        for (String key : mapTag.getAllKeys())
        {
            int colonyId = Integer.parseInt(key);
            CompoundTag fuelTag = mapTag.getCompound(key);

            int stored = fuelTag.getInt("stored");
            int cap = fuelTag.getInt("capacity");

            data.fuelData.put(colonyId, new FuelData(stored, cap));
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider)
    {
        CompoundTag mapTag = new CompoundTag();

        for (var entry : fuelData.entrySet())
        {
            CompoundTag fuelTag = new CompoundTag();

            fuelTag.putInt("stored", entry.getValue().getStored());
            fuelTag.putInt("capacity", entry.getValue().getCapacity());

            mapTag.put(String.valueOf(entry.getKey()), fuelTag);
        }

        compoundTag.put("fuel", mapTag);
        return compoundTag;
    }

    public FuelData getOrCreate(int colonyId)
    {
        return fuelData.computeIfAbsent(colonyId, id -> new FuelData(0, 200000));
    }

    public FuelData get(int colonyId)
    {
        return fuelData.get(colonyId);
    }
}