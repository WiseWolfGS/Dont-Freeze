package net.WWGS.dontfreeze.core.colony.fuel.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

public interface ColonyCoreResolver {
    @Nullable
    BlockPos resolve(ServerLevel level, int colonyId);
}
