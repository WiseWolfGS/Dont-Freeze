package net.WWGS.dontfreeze.common.block.entity;

import net.WWGS.dontfreeze.common.block.BlockGeneratorCore;
import net.WWGS.dontfreeze.common.menu.GeneratorCoreMenu;
import net.WWGS.dontfreeze.common.registry.DFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class BlockGeneratorCoreEntity extends BlockEntity implements MenuProvider {

    private boolean active;
    private int burnTicks;

    public BlockGeneratorCoreEntity(BlockPos pos, BlockState blockState) {
        super(DFBlockEntities.BLOCK_GENERATOR_CORE.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockGeneratorCoreEntity be) {
        if (level.isClientSide) return;

        if (be.active) {
            be.burnTicks--;
            if (be.burnTicks <= 0) {
                be.active = false;
                level.setBlock(
                        pos,
                        state.setValue(BlockGeneratorCore.LIT, false),
                        Block.UPDATE_CLIENTS
                );
            }
            be.setChanged();
        }

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.dontfreeze.generator_core");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new GeneratorCoreMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Active", active);
        tag.putInt("BurnTicks", burnTicks);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.loadAdditional(tag, provider);
        active = tag.getBoolean("Active");
        burnTicks = tag.getInt("BurnTicks");
    }

    public boolean isActive() {
        return active;
    }

    public int getBurnTicks() {
        return burnTicks;
    }

    public void startBurn(int ticks) {
        this.burnTicks = ticks;
        this.active = true;

        if (level != null && !level.isClientSide) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(BlockGeneratorCore.LIT, true),
                    Block.UPDATE_CLIENTS
            );
        }
        setChanged();
    }
}
