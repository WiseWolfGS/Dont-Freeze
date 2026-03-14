package wwgs.dontfreeze.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesRaider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static wwgs.dontfreeze.api.util.CompatUtils.hasColdSweat;

@Mixin(value = AbstractEntityMinecoloniesRaider.class, remap = false)
public abstract class RaiderArmorScalingMixin {

    @Shadow
    public abstract IColony getColony();

    @Inject(method = "registerWithColony()V", at = @At("TAIL"))
    private void mc_addon_applyArmor(CallbackInfo ci) {
        var self = (AbstractEntityMinecoloniesRaider)(Object)this;
        IColony colony = self.getColony();
        if (colony == null) return;

        int raidLevel = colony.getRaiderManager().getColonyRaidLevel();
        int d = dontFreeze$calcDFromRaidLevel(raidLevel * 10); // 0..100

        // Cold Sweat 없으면 가죽
        if (!hasColdSweat()) {
            dontFreeze$forceLeatherNoHelmet(self);
            return;
        }

        // 티어 확률(추천): 호글린 1% + 염소(2d)% + 나머지 가죽
        int roll = self.getRandom().nextInt(100); // 0..99

        if (roll == 0) {
            // hoglin set (모자 제외)
            dontFreeze$forceSetNoHelmet(self,
                    "cold_sweat:hoglin_chestplate",
                    "cold_sweat:hoglin_leggings",
                    "cold_sweat:hoglin_boots"
            );
            return;
        }

        int goatChance = Math.min(99, 2 * d);
        if (roll < 1 + goatChance) {
            dontFreeze$forceSetNoHelmet(self,
                    "cold_sweat:goat_fur_chestplate",
                    "cold_sweat:goat_fur_leggings",
                    "cold_sweat:goat_fur_boots"
            );
        } else {
            dontFreeze$forceLeatherNoHelmet(self);
        }
    }

    @Unique
    private static void dontFreeze$forceLeatherNoHelmet(AbstractEntityMinecoloniesRaider mob) {
        mob.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        mob.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(Items.LEATHER_LEGGINGS));
        mob.setItemSlot(EquipmentSlot.FEET,  new ItemStack(Items.LEATHER_BOOTS));
    }

    @Unique
    private static void dontFreeze$forceSetNoHelmet(AbstractEntityMinecoloniesRaider mob, String chest, String legs, String boots) {
        mob.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        Item c = dontFreeze$getItemOrNull(chest);
        Item l = dontFreeze$getItemOrNull(legs);
        Item b = dontFreeze$getItemOrNull(boots);

        if (c == null || l == null || b == null) {
            dontFreeze$forceLeatherNoHelmet(mob);
            return;
        }

        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(c));
        mob.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(l));
        mob.setItemSlot(EquipmentSlot.FEET,  new ItemStack(b));
    }

    @Unique
    private static Item dontFreeze$getItemOrNull(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
    }

    @Unique
    private static int dontFreeze$calcDFromRaidLevel(int raidLevel) {
        int min = 75;
        int max = 575;
        if (raidLevel <= min) return 0;
        if (raidLevel >= max) return 100;
        return (int) Math.round((raidLevel - min) * 100.0 / (max - min));
    }
}
