package net.WWGS.dontfreeze.common.registry;

import net.WWGS.dontfreeze.DFConstants;
import net.WWGS.dontfreeze.common.block.entity.BlockGeneratorCoreEntity;
import net.WWGS.dontfreeze.common.menu.GeneratorCoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class DFMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DFConstants.MODID);

    public static final Supplier<MenuType<GeneratorCoreMenu>> MENU_GENERATOR_CORE =
            MENUS.register("generator_core",
                    () -> IMenuTypeExtension.create(
                            (id, inv, buf) -> {
                                BlockPos pos = buf.readBlockPos();
                                BlockEntity be = inv.player.level().getBlockEntity(pos);
                                return new GeneratorCoreMenu(
                                        id, inv, (BlockGeneratorCoreEntity) be
                                );
                            }
                    )
            );

    public static void init(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
