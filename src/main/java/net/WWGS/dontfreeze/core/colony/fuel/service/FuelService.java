package net.WWGS.dontfreeze.core.colony.fuel.service;

import net.WWGS.dontfreeze.core.colony.fuel.platform.FuelSavedData;
import net.minecraft.server.level.ServerLevel;

public final class FuelService {
    private FuelService() {}

    public static void inject(ServerLevel level, int colonyId, int burnTicks) {
        if (burnTicks <= 0) return;
        FuelSavedData.get(level).addFuel(colonyId, burnTicks);
    }

    public static int getFuel(ServerLevel level, int colonyId) {
        return FuelSavedData.get(level).getFuel(colonyId);
    }

    public static void consumeFuel(ServerLevel level, int colonyId, int cost) {
        FuelSavedData.get(level).consumeFuel(colonyId, cost);
    }
}
