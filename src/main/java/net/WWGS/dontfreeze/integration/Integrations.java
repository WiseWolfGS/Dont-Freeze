package net.WWGS.dontfreeze.integration;

import net.WWGS.dontfreeze.DFConstants;
import net.WWGS.dontfreeze.integration.coldsweat.ColdSweatIntegrations;
import net.WWGS.dontfreeze.integration.minecolonies.MineColoniesIntegrations;
import net.neoforged.fml.ModList;

public final class Integrations {
    private Integrations() {}

    public static void init() {

        if (ModList.get().isLoaded(DFConstants.MINECOLONIES)) {
            MineColoniesIntegrations.init();
        } else {
            DFConstants.LOGGER.info("[{}] Integration not found: MineColonies", DFConstants.MODID);
        }

        if (ModList.get().isLoaded(DFConstants.COLDSWEAT)) {
            ColdSweatIntegrations.init();
        } else {
            DFConstants.LOGGER.info("[{}] Integration not found: Cold Sweat", DFConstants.MODID);
        }
    }
}