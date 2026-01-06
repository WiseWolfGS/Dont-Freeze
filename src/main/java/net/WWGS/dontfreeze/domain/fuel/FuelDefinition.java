package net.WWGS.dontfreeze.domain.fuel;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum FuelDefinition {
    COAL(Items.COAL, 1600),
    CHARCOAL(Items.CHARCOAL, 1600);

    private final net.minecraft.world.item.Item item;
    private final int burnTicks;

    FuelDefinition(net.minecraft.world.item.Item item, int burnTicks) {
        this.item = item;
        this.burnTicks = burnTicks;
    }

    public int burnTicks() {
        return burnTicks;
    }

    public static FuelDefinition from(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (FuelDefinition f : values()) {
            if (stack.is(f.item)) return f;
        }
        return null;
    }
}