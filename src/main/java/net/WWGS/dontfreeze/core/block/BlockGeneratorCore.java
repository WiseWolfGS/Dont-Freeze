package net.WWGS.dontfreeze.core.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.WWGS.dontfreeze.api.colony.heat.HeatTier;
import net.WWGS.dontfreeze.core.colony.fuel.menu.MenuGeneratorCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class BlockGeneratorCore extends Block {
    private static final Component TITLE = Component.translatable("menu.dontfreeze.generator_core");
    public static final MapCodec<BlockGeneratorCore> CODEC = simpleCodec(BlockGeneratorCore::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<HeatTier> HEAT_TIER = EnumProperty.create("heat_tier", HeatTier.class);
    private static final Logger LOGGER = LogUtils.getLogger();

    public BlockGeneratorCore(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false).setValue(HEAT_TIER, HeatTier.LOW));
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        openMenu(player, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        openMenu(player, pos);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, HEAT_TIER);
    }

    private static void openMenu(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer sp) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new MenuGeneratorCore(id, inv, pos),
                    TITLE
            );

            // ✅ 핵심: 메뉴 열 때 pos를 buf로 같이 보냄 (클라 메뉴 생성자에서 읽는다)
            sp.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
    }
}
