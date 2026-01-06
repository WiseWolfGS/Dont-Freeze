package net.WWGS.dontfreeze.compat;

import net.WWGS.dontfreeze.Config;
import net.WWGS.dontfreeze.compat.coldsweat.ColdSweatCompat;
import net.WWGS.dontfreeze.compat.minecolonies.MineColoniesCompat;
import net.neoforged.fml.ModList;

public final class CompatBootstrap {
    private CompatBootstrap() {}

    public static void init() {
        if (ModList.get().isLoaded(Config.COLD_SWEAT)) {
            ColdSweatCompat.init();
        }
        if (ModList.get().isLoaded(Config.MINECOLONIES)) {
            MineColoniesCompat.init();
        }
    }
}
