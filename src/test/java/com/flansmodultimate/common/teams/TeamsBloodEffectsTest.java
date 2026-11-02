package com.flansmodultimate.common.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.*;

class TeamsBloodEffectsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void newAndLegacyWorldsEnableBloodWhileExplicitOffSurvivesReload() throws ReflectiveOperationException
    {
        var instance = TeamsManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previous = instance.get(null);
        try
        {
            TeamsManager manager = new TeamsManager();
            assertTrue(manager.isBloodEffects());
            var load = TeamsManager.class.getDeclaredMethod("loadRuntime", CompoundTag.class);
            load.setAccessible(true);
            CompoundTag legacy = new CompoundTag();
            legacy.putBoolean("movie_prop", false);
            load.invoke(manager, legacy);
            assertTrue(manager.isBloodEffects());
            TeamsSavedData saved = new TeamsSavedData();
            var data = TeamsManager.class.getDeclaredField("savedData");
            data.setAccessible(true);
            data.set(manager, saved);
            manager.setBloodEffects(false);
            assertFalse(saved.runtime.getBoolean("blood_effects"));
            TeamsManager restored = new TeamsManager();
            load.invoke(restored, saved.runtime);
            assertFalse(restored.isBloodEffects());
            load.invoke(restored, new CompoundTag());
            assertTrue(restored.isBloodEffects());
            data.set(restored, saved);
            restored.setBloodEffects(true);
            assertTrue(saved.runtime.getBoolean("blood_effects"));
        }
        finally
        {
            instance.set(null, previous);
        }
    }
}
