package net.WWGS.dontfreeze.api.colony.heat;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;


public enum HeatTier implements StringRepresentable {
    LOW("low"), MID("mid"), HIGH("high");
    private final String name;

    HeatTier(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
