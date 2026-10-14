package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TeamsObjectiveServerSafetyTest
{
    private static final String[] RELOADED = {"com.flansmodultimate.common.teams.Capture", "com.flansmodultimate.common.teams.Objective", "com.flansmodultimate.common.teams.DeployLayoutEntry",
        "com.flansmodultimate.common.teams.ClassLimits", "com.flansmodultimate.network.client.teams.PacketTeamsObjectives", "com.flansmodultimate.network.client.teams.PacketRankUp",
        "com.flansmodultimate.network.server.teams.PacketDeploy", "com.flansmodultimate.network.server.teams.PacketCapturePointEditAction"};

    @Test
    void objectivePacketsAndRulesLoadWithoutMinecraftClientClasses() throws Exception
    {
        URL classes = CaptureRules.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes}, getClass().getClassLoader())
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("com.mojang.blaze3d.") || name.startsWith("com.flansmodultimate.client."))
                    throw new ClassNotFoundException("Client classes are unavailable on a dedicated server: " + name);
                for (String prefix : RELOADED)
                {
                    if (!name.startsWith(prefix))
                        continue;
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
            for (String type : new String[]{"common.teams.CaptureRules", "common.teams.CaptureScan", "common.teams.ObjectiveSnapshot", "common.teams.ClassLimits"})
                assertDoesNotThrow(() -> Class.forName("com.flansmodultimate." + type, true, loader));
            for (String type : new String[]{"network.client.teams.PacketTeamsObjectives", "network.client.teams.PacketRankUp", "network.server.teams.PacketDeploySpawnSelect",
                "network.server.teams.PacketDeployLayoutSave", "network.server.teams.PacketCapturePointEditAction"})
                assertDoesNotThrow(() -> Class.forName("com.flansmodultimate." + type, true, loader).getDeclaredConstructor().newInstance());
        }
    }
}
