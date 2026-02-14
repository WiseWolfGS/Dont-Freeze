package net.WWGS.dontfreeze.core.colony.fuel.platform;

import net.WWGS.dontfreeze.DFConfig;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.api.colony.ColonyQuery;
import net.WWGS.dontfreeze.api.util.QueryUtils;
import net.WWGS.dontfreeze.core.block.BlockGeneratorCore;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelService;
import net.WWGS.dontfreeze.core.colony.fuel.service.FuelCostCalculator;
import net.WWGS.dontfreeze.core.colony.fuel.service.NearestToTownHallCoreResolver;
import net.WWGS.dontfreeze.core.colony.fuel.storage.ColonyCoreResolver;
import net.WWGS.dontfreeze.core.colony.heat.storage.ColonyHeatParamsStorage;
import net.WWGS.dontfreeze.core.network.payload.S2CActiveGeneratorCores;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Dontfreeze.MODID)
public final class GeneratorSystemTicker {
    private static final ColonyCoreResolver CORE_RESOLVER =
            new NearestToTownHallCoreResolver();
    private static Set<BlockPos> lastActive = Set.of();
    private static int ticker = 0;

    private GeneratorSystemTicker() {}

    private static final int PERIOD_TICKS = 20;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (++ticker % PERIOD_TICKS != 0) return;

        ServerLevel level = e.getServer().overworld();

        Map<Integer, BlockPos> activeByColony = resolveActiveCores(level);

        for (int colonyId : activeByColony.keySet()) {
            double bonus =
                    ColonyHeatParamsStorage.get(level).getParams(colonyId).bonus();

            int cost = FuelCostCalculator.compute(level, colonyId, bonus).totalCostPerSecond();

            FuelService.consumeFuel(level, colonyId, cost);
        }

        Set<BlockPos> activeCores = new HashSet<>(activeByColony.values());

        if (!activeCores.equals(lastActive)) {
            applyLitDiff(level, lastActive, activeCores);
            PacketDistributor.sendToPlayersInDimension(
                    level,
                    new S2CActiveGeneratorCores(activeCores)
            );
            lastActive = Set.copyOf(activeCores);
        }
    }

    private static Map<Integer, BlockPos> resolveActiveCores(ServerLevel level) {
        ColonyQuery colonyQuery = QueryUtils.colonyQuery();
        if (colonyQuery == null) return Map.of();

        Map<Integer, BlockPos> resolved = new HashMap<>();

        Set<Integer> colonyIds = collectColonyIds(level);

        for (int colonyId : colonyIds) {
            if (FuelService.getFuel(level, colonyId) <= 0) continue;

            BlockPos core = CORE_RESOLVER.resolve(level, colonyId);
            if (core != null) {
                resolved.put(colonyId, core);
            }
        }
        return resolved;
    }

    private static Set<Integer> collectColonyIds(ServerLevel level) {
        ColonyQuery colonyQuery = QueryUtils.colonyQuery();
        if (colonyQuery == null) return Set.of();

        Set<Integer> ids = new HashSet<>();
        for (BlockPos pos : GeneratorCoreRegistry.get(level).getAll()) {
            Integer id = colonyQuery.findColonyIdAtPos(level, pos);
            ids.add(id);
        }
        return ids;
    }

    private static void applyLitDiff(ServerLevel level, Set<BlockPos> oldSet, Set<BlockPos> newSet) {
        for (BlockPos pos : newSet) {
            if (!oldSet.contains(pos)) setLit(level, pos, true);
        }
        for (BlockPos pos : oldSet) {
            if (!newSet.contains(pos)) setLit(level, pos, false);
        }
    }

    private static void setLit(ServerLevel level, BlockPos pos, boolean lit) {
        if (!level.isLoaded(pos)) return;

        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockGeneratorCore.LIT)) return;

        if (state.getValue(BlockGeneratorCore.LIT) == lit) return;

        level.setBlock(pos, state.setValue(BlockGeneratorCore.LIT, lit), 3);
    }
}
