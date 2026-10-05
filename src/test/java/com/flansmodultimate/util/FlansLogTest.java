package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlansLogTest
{
    @Test
    void sharedLoggingAndErrorFormattingDoNotLoadTheModOrClientClasses() throws Exception
    {
        URL classes = FlansLog.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {classes}, getClass().getClassLoader())
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name.equals("com.flansmodultimate.FlansMod") || name.startsWith("net.minecraft.client.")
                    || name.startsWith("com.mojang.blaze3d."))
                    throw new ClassNotFoundException("Logging must not initialize the mod or client: " + name);
                if (name.equals(FlansLog.class.getName()) || name.equals(LogUtils.class.getName()))
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
            Class<?> holder = Class.forName(FlansLog.class.getName(), true, loader);
            Logger logger = (Logger)holder.getField("log").get(null);
            assertEquals("com.flansmodultimate", logger.getName());
            Class<?> helper = Class.forName(LogUtils.class.getName(), true, loader);
            helper.getMethod("logErrorWithoutStacktrace", Throwable.class)
                .invoke(null, new IllegalStateException("Standalone logging test", new IllegalArgumentException("cause")));
        }
    }
}
