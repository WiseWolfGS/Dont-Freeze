package wwgs.dontfreeze.api.temperature.heat;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wwgs.dontfreeze.api.temperature.heat.tier.HeatLevel;

public interface IHeatManager
{
    @Nullable
    IHeatData getHeatData(@NotNull ServerLevel level, int colonyId);

    @NotNull
    IHeatData getOrCreateHeatData(@NotNull ServerLevel level, int colonyId);

    int addHeat(@NotNull ServerLevel level, int colonyId, int amount);

    int consumeHeat(@NotNull ServerLevel level, int colonyId, int amount);

    int getStoredHeat(@NotNull ServerLevel level, int colonyId);

    int getHeatCapacity(@NotNull ServerLevel level, int colonyId);

    HeatLevel getHeatLevel(@NotNull ServerLevel level, int colonyId);

    /**
     * 매 틱/매 초 heat 자연 감소 또는 정산
     */
    void tick(@NotNull ServerLevel level, int colonyId);
}