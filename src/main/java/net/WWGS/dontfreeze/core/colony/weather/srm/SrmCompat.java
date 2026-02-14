package net.WWGS.dontfreeze.core.colony.weather.srm;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class SrmCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String SRM_NAMESPACE = "snowrealmagic";


    public static boolean isSrmBlock(ServerLevel level, BlockState st) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
        return SRM_NAMESPACE.equals(id.getNamespace());
    }

    public static boolean tryRestoreOriginal(ServerLevel level, BlockPos pos) {
        BlockState cur = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        CompoundTag tag;
        try {
            tag = be.saveWithFullMetadata(level.registryAccess());
        } catch (Throwable t) {
            LOGGER.warn("[DontFreeze] SRM restore: failed to save BE NBT", t);
            return false;
        }

        // ✅ SRM 12.2.1(7579809): 원본이 Block:"minecraft:stone_slab" 처럼 문자열로 저장됨
        if (tag.contains("Block", Tag.TAG_STRING)) {
            String idStr = tag.getString("Block");
            ResourceLocation rl = ResourceLocation.tryParse(idStr);
            if (rl != null) {
                Block originalBlock = BuiltInRegistries.BLOCK.get(rl);
                if (originalBlock != null && originalBlock != net.minecraft.world.level.block.Blocks.AIR) {
                    BlockState restored = originalBlock.defaultBlockState();

                    // ✅ 현재 상태의 속성 중 원본에도 있는 속성은 복사 (슬랩/계단 방향 유지)
                    restored = copySharedProperties(cur, restored);

                    level.setBlock(pos, restored, 3);
                    return true;
                }
            }
        }

        // (옵션) 여기 아래에 “리플렉션 원본 getter 시도”나 “NBT 재귀 탐색”을 fallback으로 둬도 됨
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copySharedProperties(BlockState from, BlockState to) {
        for (Property p : from.getProperties()) {
            if (to.hasProperty(p)) {
                try {
                    Comparable v = from.getValue(p);
                    to = to.setValue(p, v);
                } catch (Throwable ignore) {}
            }
        }
        return to;
    }


    private static Optional<BlockState> findAnyBlockStateInNbt(ServerLevel level, Tag root) {
        final int MAX_DEPTH = 12;      // 충분히 크면서도 폭발 방지
        final int MAX_NODES = 20_000;  // 최악 케이스 방어

        try {
            var reg = level.registryAccess().registryOrThrow(Registries.BLOCK);
            return findAnyBlockStateInNbt0(reg, root, 0, new int[]{0}, MAX_DEPTH, MAX_NODES);
        } catch (Throwable t) {
            LOGGER.warn("[DontFreeze] findAnyBlockStateInNbt: registry access failed", t);
            return Optional.empty();
        }
    }

    private static Optional<BlockState> findAnyBlockStateInNbt0(
            net.minecraft.core.Registry<net.minecraft.world.level.block.Block> blockReg,
            Tag t,
            int depth,
            int[] nodes,
            int maxDepth,
            int maxNodes
    ) {
        if (t == null) return Optional.empty();
        if (depth > maxDepth) return Optional.empty();
        if (++nodes[0] > maxNodes) return Optional.empty();

        if (t instanceof CompoundTag c) {
            // BlockState NBT 포맷 후보: {Name:"...", Properties:{...}}
            if (c.contains("Name", Tag.TAG_STRING)) {
                try {
                    BlockState st = NbtUtils.readBlockState(blockReg.asLookup(), c);
                    return Optional.of(st);
                } catch (Throwable ignore) {
                    // "Name"이 있어도 blockstate가 아닐 수 있음
                }
            }

            for (String k : c.getAllKeys()) {
                try {
                    Optional<BlockState> found = findAnyBlockStateInNbt0(blockReg, c.get(k), depth + 1, nodes, maxDepth, maxNodes);
                    if (found.isPresent()) return found;
                } catch (Throwable ignore) {}
            }
            return Optional.empty();
        }

        if (t instanceof ListTag list) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                try {
                    Optional<BlockState> found = findAnyBlockStateInNbt0(blockReg, list.get(i), depth + 1, nodes, maxDepth, maxNodes);
                    if (found.isPresent()) return found;
                } catch (Throwable ignore) {}
            }
            return Optional.empty();
        }

        return Optional.empty();
    }
}
