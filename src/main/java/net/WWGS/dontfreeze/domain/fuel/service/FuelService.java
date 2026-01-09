package net.WWGS.dontfreeze.domain.fuel.service;

import net.WWGS.dontfreeze.domain.fuel.FuelDefinition;
import net.WWGS.dontfreeze.domain.fuel.storage.ColonyFuelStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class FuelService {
    private FuelService() {}

    public static void inject(ServerLevel level, int colonyId, int burnTicks) {
        if (burnTicks <= 0) {
            ColonyFuelStorage.get(level).getFuel(colonyId);
            return;
        }
        ColonyFuelStorage.get(level).addFuel(colonyId, burnTicks);
    }

    public static boolean canUseAsFuel(ItemStack stack) {
        FuelDefinition fuel = FuelDefinition.from(stack);
        return fuel != null;
    }

    public static int getBurnTicksForOneItem(ItemStack stack) {
        FuelDefinition fuel = FuelDefinition.from(stack);
        assert fuel != null;
        return fuel.burnTicks();
    }
}
