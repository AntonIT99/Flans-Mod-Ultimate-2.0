package com.flansmodultimate.platform;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.junit.jupiter.api.extension.Extension;

/**
 * Gives plain JUnit the loader side before any test class runs. {@code FMLEnvironment} copies the side from
 * {@code FMLLoader} once, when it is first loaded, and NeoForge's enchantment iteration reads that copy; a test that
 * loaded it earlier would otherwise leave it unset for every later test. Registered through JUnit's extension
 * autodetection (see {@code junit-platform.properties}).
 */
public final class LoaderSideExtension implements Extension
{
    static
    {
        ensureDedicatedServer();
    }

    // FMLLoader keeps the side in a private field that only its launcher sets; no server runs here.
    @SuppressWarnings("java:S3011")
    public static void ensureDedicatedServer()
    {
        try
        {
            var dist = FMLLoader.class.getDeclaredField("dist");
            dist.setAccessible(true);
            if (dist.get(null) == null)
                dist.set(null, Dist.DEDICATED_SERVER);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Could not set the NeoForge loader side for tests", e);
        }
    }
}
