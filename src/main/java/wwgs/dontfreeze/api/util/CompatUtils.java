package wwgs.dontfreeze.api.util;

import net.neoforged.fml.ModList;

public class CompatUtils {
    public static boolean hasColdSweat() {
        return ModList.get().isLoaded("cold_sweat");
    }
    public static boolean hasMineColonies() {
        return ModList.get().isLoaded("minecolonies");
    }
    public static boolean hasSnowRealMagic() { return ModList.get().isLoaded("snowrealmagic"); }
}
