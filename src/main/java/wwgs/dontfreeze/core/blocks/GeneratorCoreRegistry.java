package wwgs.dontfreeze.core.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GeneratorCoreRegistry extends SavedData
{
    private static final String DATA_NAME = "dontfreeze_generator_cores";
    private static final String TAG_CORES = "cores";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";

    private final Set<BlockPos> cores = new HashSet<>();

    private static final Factory<GeneratorCoreRegistry> FACTORY =
            new Factory<>(GeneratorCoreRegistry::new, GeneratorCoreRegistry::load, null);

    public static GeneratorCoreRegistry get(@NotNull ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public GeneratorCoreRegistry()
    {
    }

    public static GeneratorCoreRegistry load(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider)
    {
        GeneratorCoreRegistry registry = new GeneratorCoreRegistry();
        ListTag list = tag.getList(TAG_CORES, Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag coreTag = list.getCompound(i);
            int x = coreTag.getInt(TAG_X);
            int y = coreTag.getInt(TAG_Y);
            int z = coreTag.getInt(TAG_Z);
            registry.cores.add(new BlockPos(x, y, z));
        }

        return registry;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider)
    {
        ListTag list = new ListTag();

        for (BlockPos pos : cores)
        {
            CompoundTag coreTag = new CompoundTag();
            coreTag.putInt(TAG_X, pos.getX());
            coreTag.putInt(TAG_Y, pos.getY());
            coreTag.putInt(TAG_Z, pos.getZ());
            list.add(coreTag);
        }

        tag.put(TAG_CORES, list);
        return tag;
    }

    public void add(@NotNull BlockPos pos)
    {
        if (cores.add(pos.immutable()))
        {
            setDirty();
        }
    }

    public void remove(@NotNull BlockPos pos)
    {
        if (cores.remove(pos))
        {
            setDirty();
        }
    }

    public boolean contains(@NotNull BlockPos pos)
    {
        return cores.contains(pos);
    }

    public @NotNull Set<BlockPos> getAll()
    {
        return Collections.unmodifiableSet(cores);
    }
}