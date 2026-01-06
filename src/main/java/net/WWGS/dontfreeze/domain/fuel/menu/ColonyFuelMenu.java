package net.WWGS.dontfreeze.domain.fuel.menu;

import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.WWGS.dontfreeze.domain.fuel.service.FuelService;
import net.WWGS.dontfreeze.domain.fuel.storage.ColonyFuelStorage;
import net.WWGS.dontfreeze.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ColonyFuelMenu extends AbstractContainerMenu {
    public static final int FUEL_SLOT_INDEX = 0;
    private int displayFuelTicks = 0;
    private final Player player;

    private final Container fuelInv = new SimpleContainer(1);
    private final BlockPos townHallPos;

    private boolean handlingFuelSlot = false;
    private long lastConsumeServerTick = -1;

    // ===== 서버용 생성자 =====
    public ColonyFuelMenu(int id, Inventory playerInv, BlockPos townHallPos) {
        super(ModMenus.COLONY_FUEL.get(), id);
        this.townHallPos = townHallPos;
        this.player = playerInv.player;

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return displayFuelTicks;
            }

            @Override
            public void set(int value) {
                displayFuelTicks = value;
            }
        });

        // 연료 슬롯(위 1칸)
        this.addSlot(new Slot(fuelInv, FUEL_SLOT_INDEX, 130, 112) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // 연료로 쓸 수 있으면 true
                return FuelService.canUseAsFuel(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 64;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                handleFuelSlotNow();
            }
        });

        // 플레이어 인벤토리
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 58 + col * 18, 141 + row * 18));

        // 핫바
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInv, col, 58 + col * 18, 199));
    }

    // ===== 클라용(버퍼) 생성자 =====
    public ColonyFuelMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, buf.readBlockPos());
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (player.level().isClientSide) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        ItemStack stack = fuelInv.getItem(FUEL_SLOT_INDEX);
        if (stack.isEmpty()) return;

        // 타운홀 → colonyId
        Integer colonyId = MineColoniesCompat.getColonyIdFromTownHall(serverLevel, townHallPos);
        if (colonyId == null) return;

        // 스택 전체를 주입 (아이템 1개당 burnTicks 계산해서 누적)
        int totalTicks = 0;
        int count = stack.getCount();

        for (int i = 0; i < count; i++) {
            int ticks = FuelService.getBurnTicksForOneItem(stack); // ← 너 로직에 맞춰 구현/연결
            if (ticks <= 0) break;
            totalTicks += ticks;
        }

        if (totalTicks > 0) {
            FuelService.inject(serverLevel, colonyId, totalTicks);
            stack.shrink(count); // 전부 소모(부분만 주입하려면 위 루프/소모를 조절)
        }
    }

    /**
     * 쉬프트클릭 이동(연료 슬롯 <-> 플레이어 인벤)
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return empty;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        // 0번은 연료 슬롯
        if (index == FUEL_SLOT_INDEX) {
            // 연료 슬롯 → 플레이어 인벤(1..끝)
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return empty;
        } else {
            // 플레이어 인벤 → 연료 슬롯
            if (FuelService.canUseAsFuel(stack)) {
                if (!this.moveItemStackTo(stack, FUEL_SLOT_INDEX, FUEL_SLOT_INDEX + 1, false)) return empty;
            } else {
                return empty; // 연료가 아니면 이동 없음
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == copy.getCount()) return empty;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public void broadcastChanges() {
        handleFuelSlotNow();

        super.broadcastChanges();

        if (!(player instanceof ServerPlayer sp)) return;

        ServerLevel level = sp.serverLevel();

        Integer colonyId = MineColoniesCompat.getColonyIdFromTownHall(level, townHallPos);
        if (colonyId == null) {
            displayFuelTicks = 0;
            return;
        }

        long fuel = ColonyFuelStorage.get(level).getFuel(colonyId);

        // GUI 표시용으로 clamp / cast
        displayFuelTicks = (int) Math.min(fuel, Integer.MAX_VALUE);
    }

    private void handleFuelSlotNow() {
        // 서버에서만
        if (player.level().isClientSide) return;

        long nowTick = Objects.requireNonNull(player.level().getServer()).getTickCount();
        if (nowTick == lastConsumeServerTick) return;

        if (handlingFuelSlot) return;

        handlingFuelSlot = true;
        try {
            ItemStack stack = fuelInv.getItem(FUEL_SLOT_INDEX);
            if (stack.isEmpty()) return;

            if (!(player.level() instanceof ServerLevel level)) return;

            Integer colonyId = MineColoniesCompat.getColonyIdFromTownHall(level, townHallPos);
            if (colonyId == null) return;

            int perItem = FuelService.getBurnTicksForOneItem(stack);
            if (perItem <= 0) return;

            FuelService.inject(level, colonyId, perItem);

            stack.shrink(1);
            fuelInv.setChanged(); // 슬롯 갱신

            lastConsumeServerTick = nowTick;

        } finally {
            handlingFuelSlot = false;
        }
    }

    public BlockPos getTownHallPos() {
        return townHallPos;
    }

    public int getDisplayFuelTicks() {
        return displayFuelTicks;
    }
}
