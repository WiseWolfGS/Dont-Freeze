package net.WWGS.dontfreeze.api.client.gui;

import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.core.colony.fuel.menu.MenuGeneratorCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public final class DFMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Dontfreeze.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<MenuGeneratorCore>> MENU_GENERATOR_CORE = MENUS.register("generator_core", () -> IMenuTypeExtension.create(MenuGeneratorCore::new));

    private DFMenus() {
        throw new IllegalStateException("Tried to initialize: DFMenus but this is a Utility class.");
    }

    @NotNull
    public static MenuType<?>[] getMenus() {
        return new MenuType[] {
                MENU_GENERATOR_CORE.get()
        };
    }

    public static void init(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
