package net.WWGS.dontfreeze.api.colony.building.view;

import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.core.colony.buildings.views.AbstractBuildingView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GeneratorView extends AbstractBuildingView {
    private boolean displayShelfContents = true;
    private final List<ItemStack> shelfItems = new ArrayList<>();

    public GeneratorView(final IColonyView colony, final BlockPos pos) {
        super(colony, pos);
    }

    @Override
    public void deserialize(@NotNull final RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);

        CompoundTag compound = buf.readNbt();
        if (compound != null)
        {
            displayShelfContents = compound.getBoolean("displayShelfContents");

            shelfItems.clear();
            for (int i = 0; compound.contains("ritualItem" + i); i++)
            {
                String id = compound.getString("ritualItem" + i);
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id)));
                shelfItems.add(stack);
            }
        }
    }

    public boolean shouldDisplayShelfContents()
    {
        return displayShelfContents;
    }

    public List<ItemStack> getShelfItems()
    {
        return shelfItems;
    }
}
