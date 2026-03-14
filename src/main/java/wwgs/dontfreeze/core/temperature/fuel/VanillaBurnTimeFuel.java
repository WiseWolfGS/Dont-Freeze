package wwgs.dontfreeze.core.temperature.fuel;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;
import wwgs.dontfreeze.api.temperature.fuel.IFuel;

public class VanillaBurnTimeFuel implements IFuel
{
    @Override
    public boolean matches(@NotNull ItemStack stack)
    {
        return getFuelValue(stack) > 0;
    }

    @Override
    public int getFuelValue(@NotNull ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return 0;
        }

        try
        {
            return stack.getBurnTime(RecipeType.SMELTING);
        }
        catch (Throwable ignored)
        {
            return 0;
        }
    }

    @Override
    public @NotNull String getId()
    {
        return "vanilla_burn_time";
    }
}