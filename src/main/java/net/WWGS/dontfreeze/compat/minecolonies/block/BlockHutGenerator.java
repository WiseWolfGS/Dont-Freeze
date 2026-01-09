package net.WWGS.dontfreeze.compat.minecolonies.block;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.compat.minecolonies.registry.DFMineColoniesBuildings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class BlockHutGenerator extends AbstractBlockHut<BlockHutGenerator>
{
    public BlockHutGenerator(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level.isClientSide) return;

        BlockPos corePos = pos.above(1);

        level.setBlock(corePos, DontFreeze.GENERATOR_CORE_BLOCK.get().defaultBlockState(), 3);
    }

    @Override
    public @NotNull String getHutName() { return "generator"; }

    @Override
    public BuildingEntry getBuildingEntry() { return DFMineColoniesBuildings.GENERATOR.get(); }
}
