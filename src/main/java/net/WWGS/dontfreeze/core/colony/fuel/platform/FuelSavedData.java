package net.WWGS.dontfreeze.core.colony.fuel.platform;

import net.WWGS.dontfreeze.DFConfig;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.colony.fuel.storage.ColonyFuelRepository;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public final class FuelSavedData extends SavedData implements ColonyFuelRepository {
    private static final String DATA_NAME = Dontfreeze.MODID + "_colony_fuel";
    private static final String TAG_FUEL_MAP = "fuelTicks";

    private final Map<Integer, Integer> fuelTicksByColony = new HashMap<>();

    public FuelSavedData() {

    }

    private static final Factory<FuelSavedData> FACTORY = new Factory<>(FuelSavedData::new, FuelSavedData::load, null);

    public static FuelSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    @Override
    public int getFuel(int colonyId) {
        return fuelTicksByColony.getOrDefault(colonyId, 0);
    }

    @Override
    public Set<Integer> getColoniesWithFuel() {
        return fuelTicksByColony.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public void addFuel(int colonyId, int addTicks) {
        if (addTicks <= 0) return;

        int cur = getFuel(colonyId);
        if (cur == DFConfig.maxFuelTicks) return;

        int next = clamp(cur + addTicks, 0, DFConfig.maxFuelTicks);
        if (next != cur) {
            fuelTicksByColony.put(colonyId, next);
            setDirty();
        }
    }

    @Override
    public void consumeFuel(int colonyId, int consumeTicks) {
        if (consumeTicks <= 0) return;

        int cur = getFuel(colonyId);
        int next = max(0, cur - consumeTicks);

        if (next != cur) {
            if (next == 0) fuelTicksByColony.remove(colonyId);
            else fuelTicksByColony.put(colonyId, next);
            setDirty();
        }
    }

    @Override
    public void setFuel(int colonyId, int ticks) {
        int v = clamp(ticks, 0, DFConfig.maxFuelTicks);
        if (v == 0) fuelTicksByColony.remove(colonyId);
        else fuelTicksByColony.put(colonyId, v);
        setDirty();
    }

    @Override
    public boolean hasFuel(int colonyId) {
        return getFuel(colonyId) > 0;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        CompoundTag mapTag = new CompoundTag();
        for (var e : fuelTicksByColony.entrySet()) {
            mapTag.putInt(Integer.toString(e.getKey()), e.getValue());
        }
        tag.put(TAG_FUEL_MAP, mapTag);
        return tag;
    }

    private static FuelSavedData load(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        FuelSavedData data = new FuelSavedData();
        CompoundTag mapTag = tag.getCompound(TAG_FUEL_MAP);

        for (String key : mapTag.getAllKeys()) {
            try {
                int colonyId = Integer.parseInt(key);
                int ticks = mapTag.getInt(key);
                if (ticks > 0) {
                    data.fuelTicksByColony.put(colonyId, min(ticks, DFConfig.maxFuelTicks));
                }
            } catch (NumberFormatException ignored) {
                // ignore invalid keys
            }
        }
        return data;
    }
}
