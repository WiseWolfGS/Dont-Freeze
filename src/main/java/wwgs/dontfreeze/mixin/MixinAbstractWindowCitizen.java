package wwgs.dontfreeze.mixin;

import com.ldtteam.blockui.PaneBuilders;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.citizen.AbstractWindowCitizen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wwgs.dontfreeze.core.client.gui.citizen.MigrateWindowCitizen;

@Mixin(value = AbstractWindowCitizen.class, remap = false)
public abstract class MixinAbstractWindowCitizen {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void df_addMigrateTab(
            ICitizenDataView citizen,
            ResourceLocation ui,
            CallbackInfo ci)
    {
        AbstractWindowCitizen self = (AbstractWindowCitizen)(Object)this;

        var tab = self.findPaneByID("migrateTab");
        var icon = self.findPaneByID("migrateIcon");

        if (tab == null || icon == null) return;

        tab.setVisible(true);
        icon.setVisible(true);

        self.registerButton("migrateTab",
                () -> new MigrateWindowCitizen(citizen).open());
        self.registerButton("migrateIcon",
                () -> new MigrateWindowCitizen(citizen).open());

        PaneBuilders.tooltipBuilder()
                .hoverPane(icon)
                .build()
                .setText(Component.literal("주민 이적"));
    }
}