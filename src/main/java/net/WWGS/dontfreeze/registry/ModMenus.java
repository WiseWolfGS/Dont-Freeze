package net.WWGS.dontfreeze.registry;

import net.WWGS.dontfreeze.DontFreeze;
import net.WWGS.dontfreeze.domain.fuel.menu.ColonyFuelMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DontFreeze.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ColonyFuelMenu>> COLONY_FUEL =
            MENUS.register("colony_fuel", () -> IMenuTypeExtension.create(ColonyFuelMenu::new));
}
