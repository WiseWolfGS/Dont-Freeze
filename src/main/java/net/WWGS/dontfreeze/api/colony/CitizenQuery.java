package net.WWGS.dontfreeze.api.colony;

import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;

public interface CitizenQuery {
    Integer getColonyIdByCitizen(ICivilianData civilianData);
    void migrateCitizen(IColony colony, ICivilianData civilianData);
}
