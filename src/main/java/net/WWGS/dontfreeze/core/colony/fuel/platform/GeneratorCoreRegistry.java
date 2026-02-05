package net.WWGS.dontfreeze.core.colony.fuel.platform;

import net.WWGS.dontfreeze.Dontfreeze;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class GeneratorCoreRegistry extends SavedData {
    private static final String NAME = Dontfreeze.MODID + "_generator_core_positions";
    private static final String KEY = "positions";

    private final Set<BlockPos> positions = new HashSet<>();

    private static final Factory<GeneratorCoreRegistry> FACTORY =
            new Factory<>(GeneratorCoreRegistry::new, GeneratorCoreRegistry::load);

    public static GeneratorCoreRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private GeneratorCoreRegistry() {}

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        ListTag list = new ListTag();
        for (BlockPos p : positions) {
            CompoundTag ct = new CompoundTag();
            ct.putInt("x", p.getX());
            ct.putInt("y", p.getY());
            ct.putInt("z", p.getZ());
            list.add(ct);
        }
        compoundTag.put(KEY, list);
        return compoundTag;
    }

    public Set<BlockPos> getAll() {
        return positions;
    }

    public void add(BlockPos pos) {
        if (positions.add(pos.immutable())) setDirty();
    }

    public void remove(BlockPos pos) {
        if (positions.remove(pos)) setDirty();
    }

    private static GeneratorCoreRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        GeneratorCoreRegistry data = new GeneratorCoreRegistry();
        ListTag list = tag.getList(KEY, Tag.TAG_COMPOUND);

        for (Tag t : list) {
            CompoundTag ct = (CompoundTag) t;
            data.positions.add(new BlockPos(ct.getInt("x"), ct.getInt("y"), ct.getInt("z")));
        }
        return data;
    }
}
