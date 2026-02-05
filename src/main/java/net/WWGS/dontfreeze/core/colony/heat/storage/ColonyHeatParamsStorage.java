package net.WWGS.dontfreeze.core.colony.heat.storage;

import net.WWGS.dontfreeze.DFConfig;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.colony.heat.model.HeatParams;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class ColonyHeatParamsStorage extends SavedData {
    private static final String DATA_NAME = Dontfreeze.MODID + "_colony_heat_params";
    private static final String TAG_MAP = "params";

    private final Map<Integer, HeatParams> paramsByColony = new HashMap<>();

    private static final Factory<ColonyHeatParamsStorage> FACTORY =
            new Factory<>(ColonyHeatParamsStorage::new, ColonyHeatParamsStorage::load, null);

    public static ColonyHeatParamsStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public HeatParams getParams(int colonyId) {
        HeatParams p = paramsByColony.get(colonyId);
        if (p != null) return p;

        return new HeatParams(DFConfig.coreMinHeat);
    }

    public void setBonus(int colonyId, double bonus) {
        double b = clampBonus(bonus);
        paramsByColony.put(colonyId, new HeatParams(b));
        setDirty();
    }

    public void clear(int colonyId) {
        paramsByColony.remove(colonyId);
        setDirty();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        CompoundTag map = new CompoundTag();
        for (var e : paramsByColony.entrySet()) {
            CompoundTag p = new CompoundTag();
            p.putDouble("b", e.getValue().bonus());
            map.put(Integer.toString(e.getKey()), p);
        }
        tag.put(TAG_MAP, map);
        return tag;
    }

    private static ColonyHeatParamsStorage load(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        ColonyHeatParamsStorage data = new ColonyHeatParamsStorage();
        CompoundTag map = tag.getCompound(TAG_MAP);

        for (String key : map.getAllKeys()) {
            try {
                int colonyId = Integer.parseInt(key);
                CompoundTag p = map.getCompound(key);
                double b = p.getDouble("b");
                data.paramsByColony.put(colonyId, new HeatParams(clampBonus(b)));
            } catch (Exception ignored) {}
        }
        return data;
    }

    private static double clampBonus(double v) {
        return Math.min(DFConfig.coreMaxHeat, Math.max(0.0, v));
    }
}
