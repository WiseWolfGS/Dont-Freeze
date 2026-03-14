package wwgs.dontfreeze.core.temperature.fuel.ticker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import wwgs.dontfreeze.Dontfreeze;
import wwgs.dontfreeze.api.colony.building.TownHallPos;
import wwgs.dontfreeze.api.colony.building.query.BuildingQuery;
import wwgs.dontfreeze.api.util.QueryUtils;
import wwgs.dontfreeze.core.blocks.BlockGeneratorCore;
import wwgs.dontfreeze.core.blocks.GeneratorCoreRegistry;
import wwgs.dontfreeze.core.network.payload.S2CActiveGeneratorCores;
import wwgs.dontfreeze.core.temperature.fuel.FuelCostCalculator;
import wwgs.dontfreeze.core.temperature.fuel.FuelData;
import wwgs.dontfreeze.core.temperature.fuel.FuelSavedData;
import wwgs.dontfreeze.core.temperature.heat.HeatBonusSavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Dontfreeze.MODID)
public final class GeneratorSystemTicker
{
    private static final int PERIOD_TICKS = 20;

    private static Set<BlockPos> lastActive = Set.of();
    private static int ticker = 0;

    private GeneratorSystemTicker()
    {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (++ticker % PERIOD_TICKS != 0)
        {
            return;
        }

        ServerLevel level = event.getServer().overworld();
        if (level == null || !level.dimension().equals(Level.OVERWORLD))
        {
            return;
        }

        Map<Integer, BlockPos> activeByColony = resolveActiveCores(level);

        for (int colonyId : activeByColony.keySet())
        {
            double bonus = HeatBonusSavedData.get(level).getBonus(colonyId);
            int cost = FuelCostCalculator.compute(level, colonyId, bonus);
            consumeFuel(level, colonyId, cost);
        }

        Set<BlockPos> activeCores = new HashSet<>(activeByColony.values());

        applyLitDiff(level, lastActive, activeCores);

        PacketDistributor.sendToPlayersInDimension(
                level,
                new S2CActiveGeneratorCores(activeCores)
        );

        lastActive = Set.copyOf(activeCores);
    }

    /**
     * 현재 오버월드에 등록된 모든 generator core를 검사해서
     * colony별로 "town hall에 가장 가까운 core 1개"만 활성 core로 선택한다.
     */
    private static Map<Integer, BlockPos> resolveActiveCores(ServerLevel level)
    {
        BuildingQuery buildingQuery = QueryUtils.buildingQuery();
        if (buildingQuery == null)
        {
            return Map.of();
        }

        Map<Integer, BlockPos> bestCoreByColony = new HashMap<>();
        Map<Integer, Double> bestDistByColony = new HashMap<>();

        for (BlockPos corePos : GeneratorCoreRegistry.get(level).getAll())
        {
            if (!level.isLoaded(corePos))
            {
                continue;
            }

            TownHallPos townHall = buildingQuery.findNearestTownHallPos(level, corePos);
            if (townHall == null)
            {
                continue;
            }

            int colonyId = townHall.getColonyId();
            if (colonyId <= 0)
            {
                continue;
            }

            if (getFuel(level, colonyId) <= 0)
            {
                continue;
            }

            double dist2 = corePos.distSqr(townHall.getBuildingId());
            double bestDist = bestDistByColony.getOrDefault(colonyId, Double.MAX_VALUE);

            if (dist2 < bestDist)
            {
                bestDistByColony.put(colonyId, dist2);
                bestCoreByColony.put(colonyId, corePos.immutable());
            }
        }

        return bestCoreByColony;
    }

    private static int getFuel(ServerLevel level, int colonyId)
    {
        FuelData data = FuelSavedData.get(level).get(colonyId);
        return data == null ? 0 : data.getStored();
    }

    private static void consumeFuel(ServerLevel level, int colonyId, int amount)
    {
        if (amount <= 0)
        {
            return;
        }

        FuelSavedData savedData = FuelSavedData.get(level);
        FuelData data = savedData.get(colonyId);
        if (data == null)
        {
            return;
        }

        int consumed = data.consumeFuel(amount);
        if (consumed > 0)
        {
            savedData.setDirty();
        }
    }

    private static void applyLitDiff(ServerLevel level, Set<BlockPos> oldSet, Set<BlockPos> newSet)
    {
        for (BlockPos pos : newSet)
        {
            if (!oldSet.contains(pos))
            {
                setLit(level, pos, true);
            }
        }

        for (BlockPos pos : oldSet)
        {
            if (!newSet.contains(pos))
            {
                setLit(level, pos, false);
            }
        }
    }

    private static void setLit(ServerLevel level, BlockPos pos, boolean lit)
    {
        if (!level.isLoaded(pos))
        {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockGeneratorCore.LIT))
        {
            return;
        }

        if (state.getValue(BlockGeneratorCore.LIT) == lit)
        {
            return;
        }

        level.setBlock(pos, state.setValue(BlockGeneratorCore.LIT, lit), 3);
    }
}