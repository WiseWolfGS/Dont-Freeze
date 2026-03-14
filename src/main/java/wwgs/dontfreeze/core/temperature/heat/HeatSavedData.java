package wwgs.dontfreeze.core.temperature.heat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class HeatSavedData extends SavedData
{
    private static final String DATA_NAME = "dontfreeze_heat";

    private final Map<Integer, HeatData> heatData = new HashMap<>();
    private static final Factory<HeatSavedData> FACTORY =
            new Factory<>(HeatSavedData::new, HeatSavedData::load, null);

    public static HeatSavedData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public HeatSavedData() {}

    public static HeatSavedData load(CompoundTag tag, HolderLookup.Provider provider)
    {
        HeatSavedData data = new HeatSavedData();

        CompoundTag mapTag = tag.getCompound("heat");

        for (String key : mapTag.getAllKeys())
        {
            int colonyId = Integer.parseInt(key);
            CompoundTag heatTag = mapTag.getCompound(key);

            int stored = heatTag.getInt("stored");
            int capacity = heatTag.getInt("capacity");

            data.heatData.put(colonyId, new HeatData(stored, capacity));
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider)
    {
        CompoundTag mapTag = new CompoundTag();

        for (var entry : heatData.entrySet())
        {
            CompoundTag heatTag = new CompoundTag();
            heatTag.putInt("stored", entry.getValue().getStored());
            heatTag.putInt("capacity", entry.getValue().getCapacity());
            mapTag.put(String.valueOf(entry.getKey()), heatTag);
        }

        compoundTag.put("heat", mapTag);
        return compoundTag;
    }

    public HeatData getOrCreate(int colonyId)
    {
        return heatData.computeIfAbsent(colonyId, id -> new HeatData(0, 100000));
    }
}