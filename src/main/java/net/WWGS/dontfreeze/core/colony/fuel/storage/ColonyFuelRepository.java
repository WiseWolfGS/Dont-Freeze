package net.WWGS.dontfreeze.core.colony.fuel.storage;

import java.util.Set;

public interface ColonyFuelRepository {
    int getFuel(int colonyId);

    default boolean hasFuel(int colonyId) {
        return getFuel(colonyId) > 0;
    }

    Set<Integer> getColoniesWithFuel();

    void addFuel(int colonyId, int addTicks);

    void consumeFuel(int colonyId, int consumeTicks);

    void setFuel(int colonyId, int ticks);
}

