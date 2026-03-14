package wwgs.dontfreeze.mixin;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesRaider;
import com.minecolonies.api.entity.mobs.RaiderMobUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RaiderMobUtils.class, remap = false)
public final class RaiderMobUtilsSpawnRedirectMixin
{
    @Redirect(
            method = "spawn(Lnet/minecraft/world/entity/EntityType;ILnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lcom/minecolonies/api/colony/IColony;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/api/entity/mobs/AbstractEntityMinecoloniesRaider;absMoveTo(DDDFF)V"
            ),
            remap = false
    )
    private static void mc_addon_redirectAbsMoveTo(
            final AbstractEntityMinecoloniesRaider raider,
            final double x,
            final double y,
            final double z,
            final float yaw,
            final float pitch
    )
    {
        final Level level = raider.level();
        if (level.isClientSide())
        {
            raider.absMoveTo(x, y, z, yaw, pitch);
            return;
        }

        final BlockPos start = BlockPos.containing(x, y, z);
        final Vec3 safe = dontFreeze$findSafeSpawnVec(level, start, raider);

        raider.absMoveTo(safe.x, safe.y, safe.z, yaw, pitch);
    }

    @Unique
    private static Vec3 dontFreeze$findSafeSpawnVec(final Level level, final BlockPos start, final AbstractEntityMinecoloniesRaider raider)
    {
        // Fast path: if start is already good, keep it.
        if (dontFreeze$isStandable(level, start, raider))
        {
            return Vec3.atBottomCenterOf(start);
        }

        // Search parameters (tune if you want)
        final int horizontalRadius = 8;     // how far from start we try
        final int maxUp = 14;               // climb up through deep snow piles
        final int maxDown = 4;              // small down adjustment in case start is floating
        final int triesPerLayer = 60;       // random samples per dy layer (perf-friendly)

        final RandomSource rnd = raider.getRandom();

        // Scan upwards first (deep snow -> usually need to move up)
        for (int dy = 0; dy <= maxUp; dy++)
        {
            final BlockPos base = start.above(dy);
            // a few random samples per layer are usually enough
            for (int t = 0; t < triesPerLayer; t++)
            {
                final int dx = rnd.nextInt(horizontalRadius * 2 + 1) - horizontalRadius;
                final int dz = rnd.nextInt(horizontalRadius * 2 + 1) - horizontalRadius;

                BlockPos p = base.offset(dx, 0, dz);

                // avoid unloaded areas
                if (!level.isLoaded(p)) continue;

                // Try a small downward adjustment too (helps if base is in mid-air)
                for (int dd = 0; dd <= maxDown; dd++)
                {
                    final BlockPos candidate = p.below(dd);
                    if (!level.isLoaded(candidate)) continue;

                    if (dontFreeze$isStandable(level, candidate, raider))
                    {
                        return Vec3.atBottomCenterOf(candidate);
                    }
                }
            }
        }

        // If we couldn't find a safe spot, fall back to "surface-ish":
        // climb until the entity fits, without moving horizontally.
        BlockPos p = start;
        for (int i = 0; i < maxUp; i++)
        {
            if (dontFreeze$isStandable(level, p, raider))
            {
                return Vec3.atBottomCenterOf(p);
            }
            p = p.above();
        }

        // Absolute fallback: original
        return new Vec3(dontFreeze$xzCenter(start.getX()), start.getY(), dontFreeze$xzCenter(start.getZ()));
    }

    @Unique
    private static double dontFreeze$xzCenter(final int blockCoord)
    {
        return blockCoord + 0.5D;
    }

    @Unique
    private static boolean dontFreeze$isStandable(final Level level, final BlockPos feetPos, final AbstractEntityMinecoloniesRaider raider)
    {
        // Must have something to stand on
        if (level.getBlockState(feetPos.below()).getCollisionShape(level, feetPos.below()).isEmpty())
        {
            return false;
        }

        // Check actual collision for the entity box at this location
        final Vec3 at = Vec3.atBottomCenterOf(feetPos);

        // Use STANDING pose; raiders are normal-height mobs.
        final AABB box = raider.getDimensions(Pose.STANDING).makeBoundingBox(at);

        // Expand epsilon to reduce edge cases against thin shapes / partial blocks
        final double eps = 1.0E-4;
        final AABB epsBox = box.inflate(eps);

        return level.noCollision(raider, epsBox);
    }
}
