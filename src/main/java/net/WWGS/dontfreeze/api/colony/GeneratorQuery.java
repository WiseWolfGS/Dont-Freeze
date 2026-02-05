package net.WWGS.dontfreeze.api.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;

public interface GeneratorQuery {
    Set<BlockPos> getGeneratorHutPositions(ServerLevel level, int colonyId);
}
