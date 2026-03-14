package wwgs.dontfreeze.api.temperature.heat;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public interface IHeat
{
    int getHeatValue(@NotNull ServerLevel level, int colonyId);

    default boolean hasHeat(@NotNull ServerLevel level, int colonyId)
    {
        return getHeatValue(level, colonyId) > 0;
    }

    default @NotNull String getId()
    {
        return this.getClass().getSimpleName();
    }
}