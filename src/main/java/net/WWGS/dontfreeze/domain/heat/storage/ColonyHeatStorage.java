package net.WWGS.dontfreeze.domain.heat.storage;

import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.domain.heat.model.HeatParams;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class ColonyHeatStorage extends SavedData {
    private static final String DATA_NAME = DontFreeze.MODID + "_colony_heat";
    private static final String TAG_MAP = "params";
    private static final String TAG_TOWNHALL = "townhalls";
    private final Map<Integer, BlockPos> townHallByColony = new HashMap<>();


    private final Map<Integer, HeatParams> paramsByColony = new HashMap<>();

    private static final Factory<ColonyHeatStorage> FACTORY =
            new Factory<>(ColonyHeatStorage::new, ColonyHeatStorage::load, null);

    public static ColonyHeatStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public HeatParams getParams(int colonyId) {
        HeatParams p = paramsByColony.get(colonyId);
        if (p != null) return p;
        return new HeatParams(
                6,
                2.0
        );
    }

    public void setParams(int colonyId, int radiusChunks, double bonus) {
        // 상한/하한을 clamp
        int r = clamp(radiusChunks);
        double b = clamp(bonus);

        paramsByColony.put(colonyId, new HeatParams(r, b));
        setDirty();
    }

    public void clearParams(int colonyId) {
        paramsByColony.remove(colonyId);
        setDirty();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        CompoundTag map = new CompoundTag();
        CompoundTag townhalls = new CompoundTag();
        for (var e : paramsByColony.entrySet()) {
            CompoundTag p = new CompoundTag();
            p.putInt("r", e.getValue().radiusChunks());
            p.putDouble("b", e.getValue().bonus());
            map.put(Integer.toString(e.getKey()), p);
        }
        for (var e : townHallByColony.entrySet()) {
            BlockPos p = e.getValue();
            CompoundTag t = new CompoundTag();
            t.putInt("x", p.getX());
            t.putInt("y", p.getY());
            t.putInt("z", p.getZ());
            townhalls.put(Integer.toString(e.getKey()), t);
        }
        tag.put(TAG_TOWNHALL, townhalls);
        tag.put(TAG_MAP, map);
        return tag;
    }

    private static ColonyHeatStorage load(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        ColonyHeatStorage data = new ColonyHeatStorage();
        CompoundTag map = tag.getCompound(TAG_MAP);
        for (String key : map.getAllKeys()) {
            try {
                int colonyId = Integer.parseInt(key);
                CompoundTag p = map.getCompound(key);
                int r = p.getInt("r");
                double b = p.getDouble("b");
                data.paramsByColony.put(colonyId, new HeatParams(r, b));
            } catch (Exception ignored) {}
        }
        if (tag.contains(TAG_TOWNHALL)) {
            CompoundTag townhalls = tag.getCompound(TAG_TOWNHALL);
            for (String key : townhalls.getAllKeys()) {
                try {
                    int colonyId = Integer.parseInt(key);
                    CompoundTag t = townhalls.getCompound(key);
                    int x = t.getInt("x");
                    int y = t.getInt("y");
                    int z = t.getInt("z");
                    data.townHallByColony.put(colonyId, new BlockPos(x, y, z));
                } catch (Exception ignored) {}
            }
        }
        return data;
    }

    public BlockPos getTownHallPos(int colonyId) {
        return townHallByColony.get(colonyId);
    }

    public void setTownHallPos(int colonyId, BlockPos pos) {
        if (pos == null) {
            townHallByColony.remove(colonyId);
        } else {
            townHallByColony.put(colonyId, pos.immutable());
        }
        setDirty();
    }


    private static int clamp(int v) {
        return Math.min(8, Math.max(0, v));
    }
    private static double clamp(double v) {
        return Math.min(5.0, Math.max(0.0, v));
    }
}
