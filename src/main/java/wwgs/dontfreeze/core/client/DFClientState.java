package wwgs.dontfreeze.core.client;

import wwgs.dontfreeze.core.network.entry.ColonyEntry;

import java.util.List;

public final class DFClientState {
    private DFClientState() {}

    private static volatile List<ColonyEntry> colonyList = List.of();

    public static void setColonyList(List<ColonyEntry> list) {
        colonyList = List.copyOf(list);
    }

    public static List<ColonyEntry> getColonyList() {
        return colonyList;
    }
}