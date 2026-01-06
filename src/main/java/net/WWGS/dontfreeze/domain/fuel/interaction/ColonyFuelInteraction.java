package net.WWGS.dontfreeze.domain.fuel.interaction;

import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.WWGS.dontfreeze.domain.fuel.menu.ColonyFuelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.world.level.Level;

@EventBusSubscriber
public final class ColonyFuelInteraction {
    private ColonyFuelInteraction() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        // 서버에서만 처리
        if (level.isClientSide) {
            return;
        }

        if (!level.getBlockState(event.getPos()).is(DontFreeze.GENERATOR_CORE_BLOCK.get())) return;

        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        BlockPos corePos = event.getPos();
        BlockPos townHallPos = MineColoniesCompat.findNearestTownHall((ServerLevel) level, corePos);
        if (townHallPos == null) {
            sp.displayClientMessage(Component.literal("No Town Hall found nearby."), true);
            return;
        }

        sp.openMenu(
                new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Generator Core");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new ColonyFuelMenu(id, inv, townHallPos);
                    }
                },
                (RegistryFriendlyByteBuf buf) -> buf.writeBlockPos(townHallPos)
        );


        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
