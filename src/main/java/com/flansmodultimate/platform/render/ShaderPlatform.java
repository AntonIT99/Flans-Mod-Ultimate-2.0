package com.flansmodultimate.platform.render;

import com.flansmodultimate.platform.PlatformEnvironment;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Version boundary for shader mods. NeoForge 1.21.1 runs Iris itself, whose public
 * {@code net.irisshaders.iris.api.v0} API is the one Oculus keeps on Forge 1.20.1. That API is resolved reflectively,
 * so the shader mod is neither a compile nor a runtime dependency. OptiFine has no NeoForge release. Client-only.
 */
public final class ShaderPlatform
{
    private static final Logger LOG = LogUtils.getLogger();
    static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";
    /** Mod ids and names of Iris-compatible shader mods on this loader. */
    private static final String[][] SHADER_MODS = {{"iris", "Iris"}};
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
     * Whether the shader pack's shadow map is being rendered. Entity renderers then run again from the sun's view,
     * with the shadow programs bound and the main camera's projection still current.
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

    /** OptiFine only exists for Forge. */
    public static boolean isOptiFineLoaded()
    {
        return false;
    }

    /** Display name of the loaded shader mod, or {@code null} when there is none. */
    @Nullable
    public static String shaderModName()
    {
        return ShaderMod.NAME;
    }

    /** A {@code ()boolean} handle bound to a query on the live Iris API instance. */
    static MethodHandle irisApiQuery(String method) throws ReflectiveOperationException
    {
        Class<?> api = Class.forName(IRIS_API, true, ShaderPlatform.class.getClassLoader());
        Object instance = api.getMethod("getInstance").invoke(null);
        return MethodHandles.publicLookup().findVirtual(api, method, MethodType.methodType(boolean.class)).bindTo(instance);
    }

    /** Resolved on first query, once the mod list is complete. */
    private static final class ShaderMod
    {
        private static final MethodHandle SHADER_PACK_IN_USE;
        private static final MethodHandle RENDERING_SHADOW_PASS;
        @Nullable
        private static final String NAME;

        static
        {
            MethodHandle inUse = FALSE;
            MethodHandle shadowPass = FALSE;
            String name = null;
            for (String[] mod : SHADER_MODS)
            {
                if (!PlatformEnvironment.isModLoaded(mod[0]))
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
