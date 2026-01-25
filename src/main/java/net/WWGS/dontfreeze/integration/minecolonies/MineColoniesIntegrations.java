package net.WWGS.dontfreeze.integration.minecolonies;

import net.WWGS.dontfreeze.common.api.IntegrationAPIs;
import net.WWGS.dontfreeze.integration.minecolonies.bridge.MineColoniesColonyQuery;

public class MineColoniesIntegrations {
    public static void init() {
        IntegrationAPIs.registerColonyQuery(new MineColoniesColonyQuery());
    }
}
