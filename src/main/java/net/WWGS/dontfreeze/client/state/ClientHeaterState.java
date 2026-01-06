package net.WWGS.dontfreeze.client.state;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 클라이언트에서만 쓰는 “작동 중인 발전기 코어” 캐시.
 * 서버 -> 클라 S2C 패킷으로 갱신되고, 파티클/렌더/UI에서 읽기만 한다.
 */
public final class ClientHeaterState {
    private ClientHeaterState() {}

    public static final Set<BlockPos> ACTIVE_GENERATOR_CORES = ConcurrentHashMap.newKeySet();

    public static Set<BlockPos> getActiveGeneratorCoresView() {
        return Collections.unmodifiableSet(ACTIVE_GENERATOR_CORES);
    }

    public static void replaceActiveGeneratorCores(Set<BlockPos> cores) {
        ACTIVE_GENERATOR_CORES.clear();
        ACTIVE_GENERATOR_CORES.addAll(cores);
    }

    public static void clear() {
        ACTIVE_GENERATOR_CORES.clear();
    }
}
