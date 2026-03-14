package wwgs.dontfreeze.api.temperature.fuel;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface IFuel
{
    boolean matches(@NotNull ItemStack stack);
    int getFuelValue(@NotNull ItemStack stack);
    default @NotNull String getId()
    {
        return this.getClass().getSimpleName();
    }
    default boolean isValid(@NotNull ItemStack stack)
    {
        return !stack.isEmpty() && matches(stack) && getFuelValue(stack) > 0;
    }
}