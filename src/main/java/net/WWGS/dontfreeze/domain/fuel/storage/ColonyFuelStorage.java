package net.WWGS.dontfreeze.domain.fuel.storage;

import net.WWGS.dontfreeze.DontFreeze;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.max;
import static java.lang.Math.min;

public final class ColonyFuelStorage extends SavedData {

    /** 최대 연료(틱). 예: 60분 * 60초 * 20틱 = 72000 */
    public static final int MAX_FUEL_AMOUNT_MINUTES = 60 * 99;
    public static final int MAX_FUEL_TICKS = MAX_FUEL_AMOUNT_MINUTES * 60 * 20;

    private static final String DATA_NAME = DontFreeze.MODID + "_colony_fuel";
    private static final String TAG_FUEL_MAP = "fuelTicks";

    private final Map<Integer, Integer> fuelTicksByColony = new HashMap<>();

    public ColonyFuelStorage() {}

    /* -----------------------------
     * Access (ServerLevel binding)
     * ----------------------------- */

    private static final Factory<ColonyFuelStorage> FACTORY =
            new Factory<>(ColonyFuelStorage::new, ColonyFuelStorage::load, null);

    public static ColonyFuelStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    /* -----------------------------
     * Read API
     * ----------------------------- */

    public int getFuel(int colonyId) {
        return fuelTicksByColony.getOrDefault(colonyId, 0);
    }

    public boolean hasFuel(int colonyId) {
        return getFuel(colonyId) > 0;
    }

    /** 디버그/관리용: 연료가 남아있는 콜로니들 */
    public Set<Integer> getColoniesWithFuel() {
        return fuelTicksByColony.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    /* -----------------------------
     * Write API
     * ----------------------------- */

    /**
     * 연료 추가. (0 이하 입력은 무시)
     */
    public void addFuel(int colonyId, int addTicks) {
        if (addTicks <= 0) return;

        int cur = getFuel(colonyId);

        if (cur == MAX_FUEL_TICKS) {
            return;
        }

        int next = clamp(cur + addTicks);

        if (next != cur) {
            fuelTicksByColony.put(colonyId, next);
            setDirty();
        }
    }

    /**
     * 연료 소비. (0 이하 입력은 무시)
     */
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

    /**
     * 강제 설정 (디버그/관리용)
     */
    public void setFuel(int colonyId, int ticks) {
        int v = clamp(ticks);
        if (v == 0) fuelTicksByColony.remove(colonyId);
        else fuelTicksByColony.put(colonyId, v);
        setDirty();
    }

    /* -----------------------------
     * Persistence (NBT)
     * ----------------------------- */

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        CompoundTag mapTag = new CompoundTag();
        for (var e : fuelTicksByColony.entrySet()) {
            // key는 String이어야 하므로 colonyId를 문자열로
            mapTag.putInt(Integer.toString(e.getKey()), e.getValue());
        }
        tag.put(TAG_FUEL_MAP, mapTag);
        return tag;
    }

    private static ColonyFuelStorage load(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        ColonyFuelStorage data = new ColonyFuelStorage();
        CompoundTag mapTag = tag.getCompound(TAG_FUEL_MAP);

        for (String key : mapTag.getAllKeys()) {
            try {
                int colonyId = Integer.parseInt(key);
                int ticks = mapTag.getInt(key);
                if (ticks > 0) {
                    data.fuelTicksByColony.put(colonyId, min(ticks, MAX_FUEL_TICKS));
                }
            } catch (NumberFormatException ignored) {
                // 잘못된 키는 무시
            }
        }
        return data;
    }

    /* -----------------------------
     * Utils
     * ----------------------------- */

    private static int clamp(int v) {
        return min(ColonyFuelStorage.MAX_FUEL_TICKS, max(0, v));
    }
}
