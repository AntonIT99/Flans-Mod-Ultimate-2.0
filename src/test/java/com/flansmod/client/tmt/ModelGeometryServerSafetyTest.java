package com.flansmod.client.tmt;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ModelGeometryServerSafetyTest
{
    @Test
    void geometryConstructionDoesNotInitializeClientRenderingClasses() throws Exception
    {
        URL classes = ModelRendererTurbo.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {classes}, getClass().getClassLoader())
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name.startsWith("com.mojang.blaze3d.") || name.startsWith("net.minecraft.client."))
                    throw new ClassNotFoundException("Client rendering is unavailable on the server: " + name);
                if (name.startsWith("com.flansmod.") || name.startsWith("com.flansmodultimate.client."))
                {
                    synchronized (getClassLoadingLock(name))
                    {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null)
                            loaded = findClass(name);
                        if (resolve)
                            resolveClass(loaded);
                        return loaded;
                    }
                }
                return super.loadClass(name, resolve);
            }
        })
        {
            assertDoesNotThrow(() -> Class.forName("com.flansmod.client.model.ModelDefaultMuzzleFlash", true, loader)
                .getDeclaredConstructor().newInstance());
        }
    }
}
