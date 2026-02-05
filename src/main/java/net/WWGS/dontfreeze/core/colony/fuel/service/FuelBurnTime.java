package net.WWGS.dontfreeze.core.colony.fuel.service;

import net.minecraft.world.item.ItemStack;

public final class FuelBurnTime {
    private FuelBurnTime() {}

    public static int getBurnTicks(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        try {
            var m = ItemStack.class.getMethod("getBurnTime", net.minecraft.world.item.crafting.RecipeType.class);
            Object v = m.invoke(stack, new Object[] { null });
            if (v instanceof Integer i) return Math.max(0, i);
        } catch (Throwable ignored) {}

        try {
            var m = ItemStack.class.getMethod("getBurnTime");
            Object v = m.invoke(stack);
            if (v instanceof Integer i) return Math.max(0, i);
        } catch (Throwable ignored) {}

        return 0;
    }
}