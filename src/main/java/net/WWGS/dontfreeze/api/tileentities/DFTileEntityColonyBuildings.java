package net.WWGS.dontfreeze.api.tileentities;

import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class DFTileEntityColonyBuildings extends TileEntityColonyBuilding {

    public DFTileEntityColonyBuildings(BlockEntityType<? extends AbstractTileEntityColonyBuilding> type, BlockPos pos,
                                        BlockState state) {
        super(type, pos, state);
    }

    /**
     * Default constructor used to create a new TileEntity via reflection. Do not use.
     */
    public DFTileEntityColonyBuildings(final BlockPos pos, final BlockState state)
    {
        this(DFTileEntities.BUILDING.get(), pos, state);
    }

    @Override
    public void loadAdditional(@NotNull final CompoundTag compound, @NotNull final HolderLookup.Provider provider)
    {
        super.loadAdditional(compound, provider);
    }
}
