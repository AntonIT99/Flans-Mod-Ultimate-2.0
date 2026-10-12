package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CombatFeedbackServerSafetyTest
{
    @Test
    void serverFeedbackPacketsAndHooksLoadWithoutMinecraftClientClasses() throws Exception
    {
        URL classes = NearMissHandler.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes}, getClass().getClassLoader())
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("com.mojang.blaze3d."))
                    throw new ClassNotFoundException("Client classes are unavailable on a dedicated server: " + name);
                if (name.startsWith("com.flansmodultimate.network.client.effects.PacketCombatFeedback") || name.startsWith("com.flansmodultimate.network.client.effects.PacketBulletFlyby")
                    || name.startsWith("com.flansmodultimate.network.client.gun.PacketGunSecondaryModeClient") || name.startsWith("com.flansmodultimate.network.server.gun.PacketGunSecondaryMode")
                    || name.startsWith("com.flansmodultimate.common.guns.GunHandlingTiming") || name.startsWith("com.flansmodultimate.hooks.")
                    || name.startsWith("com.flansmodultimate.common.guns.NearMiss"))
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
            assertDoesNotThrow(() -> Class.forName("com.flansmodultimate.common.guns.NearMissHandler", true, loader));
            for (String type : new String[]{"network.client.effects.PacketCombatFeedback", "network.client.effects.PacketBulletFlyby", "hooks.server.ClientRenderHooksNoop",
                "hooks.server.ClientGunHooksNoop", "network.client.gun.PacketGunSecondaryModeClient", "network.server.gun.PacketGunSecondaryMode"})
                assertDoesNotThrow(() -> Class.forName("com.flansmodultimate." + type, true, loader).getDeclaredConstructor().newInstance());
        }
    }
}
