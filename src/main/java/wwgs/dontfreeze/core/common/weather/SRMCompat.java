package wwgs.dontfreeze.core.common.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import wwgs.dontfreeze.api.util.CompatUtils;

import java.lang.reflect.Method;

public final class SRMCompat
{
    private static final String SRM_NAMESPACE = "snowrealmagic";
    private static final String SRM_NAMESPACE_ALT = "snow_real_magic";

    private static boolean reflectionInitialized = false;
    private static Class<?> snowVariantClass;
    private static Method getRawMethod;
    private static Method decreaseLayerMethod;

    private SRMCompat()
    {
    }

    public static boolean isSrmBlock(ServerLevel level, BlockState state)
    {
        return isSrmBlock(state);
    }

    public static boolean isSrmBlock(BlockState state)
    {
        if (!CompatUtils.hasSnowRealMagic() || state == null)
        {
            return false;
        }

        String namespace = state.getBlock()
                .builtInRegistryHolder()
                .key()
                .location()
                .getNamespace();

        if (SRM_NAMESPACE.equals(namespace) || SRM_NAMESPACE_ALT.equals(namespace))
        {
            return true;
        }

        initReflection();
        return snowVariantClass != null && snowVariantClass.isInstance(state.getBlock());
    }

    public static boolean tryMelt(ServerLevel level, BlockPos pos, BlockState state)
    {
        if (!CompatUtils.hasSnowRealMagic())
        {
            return false;
        }

        if (!isSrmBlock(state))
        {
            return false;
        }

        // 1) 레이어형 SRM 블록은 한 층씩 천천히 줄이기
        IntegerProperty layersProp = findLayersProperty(state);
        if (layersProp != null)
        {
            int layers = state.getValue(layersProp);

            if (layers > 1)
            {
                BlockState lowered = state.setValue(layersProp, layers - 1);
                level.setBlock(pos, lowered, 2);
                return true;
            }
        }

        // 2) 마지막 단계면 SRM이 들고 있는 원래 블록 복구 시도
        if (tryRestoreOriginal(level, pos, state))
        {
            return true;
        }

        // 3) 혹시 raw 복구가 안 되면 SRM 감소 메서드 fallback
        BlockState decreased = tryInvokeDecreaseLayer(level, pos, state);
        if (decreased != null)
        {
            if (decreased.isAir())
            {
                level.removeBlock(pos, false);
            }
            else
            {
                level.setBlock(pos, decreased, 2);
            }
            return true;
        }

        // 4) 그래도 실패하면 최후 fallback
        level.removeBlock(pos, false);
        return true;
    }

    public static boolean tryRestoreOriginal(ServerLevel level, BlockPos pos)
    {
        if (!level.isLoaded(pos))
        {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return tryRestoreOriginal(level, pos, state);
    }

    public static boolean tryRestoreOriginal(ServerLevel level, BlockPos pos, BlockState state)
    {
        if (!CompatUtils.hasSnowRealMagic())
        {
            return false;
        }

        if (!isSrmBlock(state))
        {
            return false;
        }

        BlockState raw = tryGetRawState(level, pos, state);
        if (raw == null)
        {
            return false;
        }

        if (raw.isAir())
        {
            level.removeBlock(pos, false);
        }
        else
        {
            level.setBlock(pos, raw, 2);
        }
        return true;
    }

    private static IntegerProperty findLayersProperty(BlockState state)
    {
        for (var property : state.getProperties())
        {
            if (property instanceof IntegerProperty intProperty
                    && "layers".equals(intProperty.getName()))
            {
                return intProperty;
            }
        }
        return null;
    }

    private static BlockState tryGetRawState(ServerLevel level, BlockPos pos, BlockState state)
    {
        initReflection();

        if (snowVariantClass == null || getRawMethod == null)
        {
            return null;
        }

        if (!snowVariantClass.isInstance(state.getBlock()))
        {
            return null;
        }

        try
        {
            Object raw = getRawMethod.invoke(state.getBlock(), state, level, pos);
            if (raw instanceof BlockState rawState)
            {
                return rawState;
            }
        }
        catch (Throwable ignored)
        {
        }

        return null;
    }

    private static BlockState tryInvokeDecreaseLayer(ServerLevel level, BlockPos pos, BlockState state)
    {
        initReflection();

        if (snowVariantClass == null || decreaseLayerMethod == null)
        {
            return null;
        }

        if (!snowVariantClass.isInstance(state.getBlock()))
        {
            return null;
        }

        try
        {
            Object result = decreaseLayerMethod.invoke(state.getBlock(), state, level, pos, false);
            if (result instanceof BlockState blockState)
            {
                return blockState;
            }
        }
        catch (Throwable ignored)
        {
        }

        return null;
    }

    private static void initReflection()
    {
        if (reflectionInitialized)
        {
            return;
        }

        reflectionInitialized = true;

        try
        {
            snowVariantClass = Class.forName("snownee.snow.block.SnowVariant");

            getRawMethod = snowVariantClass.getMethod(
                    "srm$getRaw",
                    BlockState.class,
                    BlockGetter.class,
                    BlockPos.class
            );

            decreaseLayerMethod = snowVariantClass.getMethod(
                    "srm$decreaseLayer",
                    BlockState.class,
                    Level.class,
                    BlockPos.class,
                    boolean.class
            );
        }
        catch (Throwable ignored)
        {
            snowVariantClass = null;
            getRawMethod = null;
            decreaseLayerMethod = null;
        }
    }
}