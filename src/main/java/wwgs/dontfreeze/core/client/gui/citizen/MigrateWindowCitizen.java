package wwgs.dontfreeze.core.client.gui.citizen;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingGroup;
import com.ldtteam.blockui.views.View;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.citizen.AbstractWindowCitizen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wwgs.dontfreeze.core.client.DFClientState;
import wwgs.dontfreeze.core.network.DFNetworks;
import wwgs.dontfreeze.core.network.entry.ColonyEntry;
import wwgs.dontfreeze.core.network.payload.C2SCitizenMigrate;
import wwgs.dontfreeze.core.network.payload.C2SRequestColonyList;

import java.util.List;

public class MigrateWindowCitizen extends AbstractWindowCitizen {
    private int selectedColonyId = -1;

    private ScrollingGroup colonyListGroup;
    private View rowTemplate;

    private List<ColonyEntry> colonies = List.of();

    public MigrateWindowCitizen(ICitizenDataView citizen) {
        super(citizen, ResourceLocation.fromNamespaceAndPath("dontfreeze", "gui/citizen/migrate.xml"));

        registerButton("confirm", this::onConfirm);
        registerButton("refresh", this::requestColonyList);

        colonyListGroup = (ScrollingGroup) findPaneByID("colonyList");
        rowTemplate = (View) findPaneByID("colonyRowTemplate");

        Text title = findPaneOfTypeByID("title", Text.class);
        if (title != null) {
            title.setText(Component.translatable("dontfreeze.gui.citizen.migrate.title"));
        }

        System.out.println("[DF] template class = " + (rowTemplate == null ? "null" : rowTemplate.getClass().getName()));
        System.out.println("[DF] group children before = " + colonyListGroup.getChildren().size());

        refreshFromClientCache();
        requestColonyList();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        refreshFromClientCache();
    }

    private void refreshFromClientCache() {
        var cache = DFClientState.getColonyList();

        if (cache.size() == colonies.size()) {
            boolean same = true;
            for (int i = 0; i < cache.size(); i++) {
                if (cache.get(i).id() != colonies.get(i).id()) { same = false; break; }
            }
            if (same) return;
        }

        colonies = List.copyOf(cache);
        rebuildListUI();
    }

    private static final int MAX_ROWS = 10;

    private void rebuildListUI() {
        if (colonyListGroup == null) return;

        for (int i = 0; i < MAX_ROWS; i++) {
            Pane row = colonyListGroup.findPaneByID("row" + i);
            if (row != null) row.setVisible(false);
        }

        int shown = 0;

        for (int i = 0; i < Math.min(colonies.size(), MAX_ROWS); i++) {
            ColonyEntry entry = colonies.get(i);

            Pane row = colonyListGroup.findPaneByID("row" + i);
            if (row == null) continue;

            row.setVisible(true);

            Text name = row.findPaneOfTypeByID("colonyName" + i, Text.class);
            if (name != null) {
                name.setText(Component.literal(entry.name() + " (#" + entry.id() + ")"));
            }

            final int colonyId = entry.id();
            registerButton("select" + i, () -> {
                selectedColonyId = colonyId;
                rebuildListUI(); // 선택 상태 반영
            });

            shown++;
        }

        colonyListGroup.onUpdate();
        System.out.println("[DF] rows shown = " + shown);
    }


    private void onConfirm() {
        if (selectedColonyId <= 0) return;
        DFNetworks.sendToServer(new C2SCitizenMigrate(citizen.getEntityId(), selectedColonyId));
    }

    private void requestColonyList() {
        DFNetworks.sendToServer(new C2SRequestColonyList());
    }

    private static View copyView(View template) {
        for (String name : new String[]{"copy", "deepCopy", "copyRecursive"}) {
            try {
                var m = template.getClass().getDeclaredMethod(name);
                m.setAccessible(true);
                Object res = m.invoke(template);
                if (res instanceof View v) return v;
            } catch (Exception ignored) {
                System.out.println("[DF] View methods1: " +
                        java.util.Arrays.toString(template.getClass().getDeclaredMethods()));

            }

            try {
                var m = template.getClass().getMethod(name);
                Object res = m.invoke(template);
                if (res instanceof View v) return v;
            } catch (Exception ignored) {
                System.out.println("[DF] View methods2: " +
                        java.util.Arrays.toString(template.getClass().getDeclaredMethods()));
            }
        }
        return null;
    }
}