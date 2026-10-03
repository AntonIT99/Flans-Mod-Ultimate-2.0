package com.flansmodultimate.platform.render;

import com.flansmodultimate.platform.PlatformEnvironment;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Version boundary for shader mods. Forge 1.20.1 uses Oculus, the Forge port of Iris, which keeps Iris's public
 * {@code net.irisshaders.iris.api.v0} API. That API is resolved reflectively, so the shader mod is neither a compile
 * nor a runtime dependency. OptiFine also exists for Forge 1.20.1 but has no public shader API. Client-only.
 */
public final class ShaderPlatform
{
    private static final Logger LOG = LogUtils.getLogger();
    static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";
    /** Mod ids and names of Iris-compatible shader mods on this loader; Oculus also declares that it provides Iris. */
    private static final String[][] SHADER_MODS = {{"oculus", "Oculus"}, {"iris", "Iris"}};
    private static final MethodHandle FALSE = MethodHandles.constant(boolean.class, false);

    private ShaderPlatform() {}

    /**
     * Whether a shader pack currently replaces the vanilla world pipeline. While one is active, the shader mod
     * substitutes the pack's programs for the vanilla {@code GameRenderer} shaders, extends the vanilla entity vertex
     * formats, and masks colour and depth writes from shaders it does not know during world rendering.
     */
    public static boolean isShaderPackInUse()
    {
        try
        {
            return (boolean)ShaderMod.SHADER_PACK_IN_USE.invokeExact();
        }
        catch (Throwable ex)
        {
            return false;
        }
    }

    /**
     * Whether the shader pack's shadow map is being rendered. Entity renderers then run again from the sun's view:
     * the shadow programs are bound, the pose stack holds the sun's view and {@code RenderSystem}'s projection is the
     * shadow map's, so neither measures what the camera sees.
     */
    public static boolean isRenderingShadowPass()
    {
        try
        {
            return (boolean)ShaderMod.RENDERING_SHADOW_PASS.invokeExact();
        }
        catch (Throwable ex)
        {
            return false;
        }
    }

    /** OptiFine changes the whole render pipeline even without a shader pack, and offers no API to query it. */
    public static boolean isOptiFineLoaded()
    {
        return ShaderMod.OPTIFINE_LOADED;
    }

    /** Display name of the loaded shader mod, or {@code null} when there is none. */
    @Nullable
    public static String shaderModName()
    {
        return ShaderMod.NAME != null ? ShaderMod.NAME : ShaderMod.OPTIFINE_LOADED ? "OptiFine" : null;
    }

    /** A {@code ()boolean} handle bound to a query on the live Iris API instance. */
    static MethodHandle irisApiQuery(String method) throws ReflectiveOperationException
    {
        Class<?> api = Class.forName(IRIS_API, true, ShaderPlatform.class.getClassLoader());
        Object instance = api.getMethod("getInstance").invoke(null);
        return MethodHandles.publicLookup().findVirtual(api, method, MethodType.methodType(boolean.class)).bindTo(instance);
    }

    private static boolean isModLoaded(String modId)
    {
        try
        {
            return PlatformEnvironment.isModLoaded(modId);
        }
        catch (RuntimeException ex)
        {
            // There is no mod list outside a running game, such as in unit tests.
            return false;
        }
    }

    private static boolean classExists(String name)
    {
        try
        {
            Class.forName(name, false, ShaderPlatform.class.getClassLoader());
            return true;
        }
        catch (ClassNotFoundException | LinkageError ex)
        {
            return false;
        }
    }

    /** Resolved on first query, once the mod list is complete. */
    private static final class ShaderMod
    {
        private static final MethodHandle SHADER_PACK_IN_USE;
        private static final MethodHandle RENDERING_SHADOW_PASS;
        @Nullable
        private static final String NAME;
        private static final boolean OPTIFINE_LOADED = classExists("net.optifine.Config");

        static
        {
            MethodHandle inUse = FALSE;
            MethodHandle shadowPass = FALSE;
            String name = null;
            for (String[] mod : SHADER_MODS)
            {
                if (!isModLoaded(mod[0]))
                    continue;
                try
                {
                    inUse = irisApiQuery("isShaderPackInUse");
                    shadowPass = irisApiQuery("isRenderingShadowPass");
                    name = mod[1];
                }
                catch (ReflectiveOperationException | LinkageError | RuntimeException ex)
                {
                    inUse = shadowPass = FALSE;
                    LOG.warn("{} is loaded, but its shader API is unavailable; assuming no shader pack is active", mod[1], ex);
                }
                break;
            }
            SHADER_PACK_IN_USE = inUse;
            RENDERING_SHADOW_PASS = shadowPass;
            NAME = name;
        }
    }
}
