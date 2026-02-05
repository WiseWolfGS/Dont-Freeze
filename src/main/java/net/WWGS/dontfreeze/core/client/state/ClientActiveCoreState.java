package net.WWGS.dontfreeze.core.client.state;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ClientActiveCoreState {
    private static volatile Set<BlockPos> active = Collections.emptySet();

    private ClientActiveCoreState() {}

    public static Set<BlockPos> getActive() {
        return active;
    }

    public static void replace(Set<BlockPos> newSet) {
        // 불변 셋처럼 쓰기 위해 복사
        active = Set.copyOf(newSet);
    }
}