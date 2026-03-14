package wwgs.dontfreeze.api.colony.citizen.query;

import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;

public interface CitizenQuery {
    void migrateCitizen(IColony colony, ICivilianData civilianData);
}
