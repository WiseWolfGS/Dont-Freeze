package wwgs.dontfreeze.core.temperature.heat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class HeatBonusSavedData extends SavedData {
    private static final String DATA_NAME = "dontfreeze_heat_bonus";

    private final Map<Integer, Double> bonusByColony = new HashMap<>();

    private static final Factory<HeatBonusSavedData> FACTORY =
            new Factory<>(HeatBonusSavedData::new, HeatBonusSavedData::load, null);

    public static HeatBonusSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static HeatBonusSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        HeatBonusSavedData data = new HeatBonusSavedData();

        CompoundTag mapTag = tag.getCompound("bonus");
        for (String key : mapTag.getAllKeys()) {
            try {
                int colonyId = Integer.parseInt(key);
                double bonus = mapTag.getDouble(key);
                data.bonusByColony.put(colonyId, Math.max(0.0, bonus));
            } catch (Exception ignored) {
            }
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        CompoundTag mapTag = new CompoundTag();

        for (var entry : bonusByColony.entrySet()) {
            mapTag.putDouble(String.valueOf(entry.getKey()), entry.getValue());
        }

        tag.put("bonus", mapTag);
        return tag;
    }

    public double getBonus(int colonyId) {
        return bonusByColony.getOrDefault(colonyId, 0.0);
    }

    public void setBonus(int colonyId, double bonus) {
        bonusByColony.put(colonyId, Math.max(0.0, bonus));
        setDirty();
    }

    public void clear(int colonyId) {
        bonusByColony.remove(colonyId);
        setDirty();
    }
}
