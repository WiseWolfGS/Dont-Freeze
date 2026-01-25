package net.WWGS.dontfreeze.common.block;

import net.WWGS.dontfreeze.common.block.entity.BlockGeneratorCoreEntity;
import net.WWGS.dontfreeze.common.registry.DFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static com.minecolonies.api.blocks.interfaces.ITickableBlockMinecolonies.createTickerHelper;

public final class BlockGeneratorCore extends Block implements EntityBlock {
    private static final Component TITLE = Component.translatable("screen.dontfreeze.generator_core");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BlockGeneratorCore(Properties props) {
        super(props);
        this.registerDefaultState(this.getStateDefinition().any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BlockGeneratorCoreEntity(pos, state);
    }

    @Override
    @Nullable
    protected MenuProvider getMenuProvider(@NotNull BlockState state, Level level, @NotNull BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return (be instanceof MenuProvider mp) ? mp : null;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
                                                        @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
            MenuProvider provider = state.getMenuProvider(level, pos);
            if (provider != null) {
                player.openMenu(provider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
    ) {
        return level.isClientSide ? null
                : createTickerHelper(
                type,
                DFBlockEntities.BLOCK_GENERATOR_CORE.get(),
                BlockGeneratorCoreEntity::serverTick
        );
    }

    @Override
    public void animateTick(BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        if (!state.getValue(LIT)) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;

        if (rand.nextFloat() < 0.3f) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    x + (rand.nextDouble() - 0.5) * 0.2,
                    y,
                    z + (rand.nextDouble() - 0.5) * 0.2,
                    0.0, 0.02, 0.0
            );
        }
    }
}
