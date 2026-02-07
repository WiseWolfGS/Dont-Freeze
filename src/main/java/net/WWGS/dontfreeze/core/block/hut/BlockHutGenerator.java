package net.WWGS.dontfreeze.core.block.hut;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.WWGS.dontfreeze.api.colony.building.DFBuildings;
import net.WWGS.dontfreeze.api.tileentities.DFTileEntityColonyBuildings;
import net.WWGS.dontfreeze.core.block.DFBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockHutGenerator extends AbstractBlockHut<BlockHutGenerator> {

    public BlockHutGenerator(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull String getHutName() {
        return "generator";
    }

    @Override
    public BuildingEntry getBuildingEntry() {
        return DFBuildings.generator;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!(level instanceof ServerLevel sl)) return;

        BlockPos corePos = pos.above(1); // 원하는 위치로 조정 가능
        if (sl.isEmptyBlock(corePos)) {
            sl.setBlock(corePos, DFBlocks.BLOCK_GENERATOR_CORE.get().defaultBlockState(), 3);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        final DFTileEntityColonyBuildings building = new DFTileEntityColonyBuildings(pos, state);
        if (this.getBuildingEntry() != null && this.getBuildingEntry().getRegistryName() != null) {
            building.registryName = this.getBuildingEntry().getRegistryName();
        }
        return building;
    }
}
