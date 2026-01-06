package net.WWGS.dontfreeze.compat.minecolonies.block;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.compat.minecolonies.registry.DFMineColoniesBuildings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level.isClientSide) return;

        // hut 위에 돌 반블록 1개, 그 위에 generator_core 1개를 자동 배치
        BlockPos slabPos = pos.above();
        BlockPos corePos = pos.above(2);

        level.setBlock(slabPos, Blocks.STONE_SLAB.defaultBlockState(), 3);
        level.setBlock(corePos, DontFreeze.GENERATOR_CORE_BLOCK.get().defaultBlockState(), 3);
    }

    @Override
    public @NotNull String getHutName() { return "generator"; }

    @Override
    public BuildingEntry getBuildingEntry() { return DFMineColoniesBuildings.GENERATOR.get(); }
}
