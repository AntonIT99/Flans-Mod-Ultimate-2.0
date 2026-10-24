package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.config.ModClientConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigDefaultsTest
{
    @Test
    void restoresOnlyDisplayedValuesAndFlushesEachTargetOnce()
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        var visible = builder.define("visible", true);
        var decimal = builder.defineInRange("decimal", 0.25D, 0D, 1D);
        var hidden = builder.defineInRange("hidden", 12, 0, 100);
        builder.build();
        ConfigTarget target = editableTarget();
        ConfigDefaults defaults = new ConfigDefaults();
        defaults.add(target, visible);
        defaults.add(target, decimal);
        defaults.add(target, visible);

        assertTrue(defaults.canRestore());
        defaults.restore();

        verify(target).setWhileDragging(visible, true);
        verify(target).setWhileDragging(decimal, 0.25D);
        verify(target, never()).setWhileDragging(eq(hidden), any());
        verify(target).flush();
    }

    @Test
    void skipsLockedOrUnavailableServerSettingsWhileRestoringClientSettings()
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        var value = builder.define("value", false);
        builder.build();
        ConfigTarget client = editableTarget();
        ConfigTarget locked = editableTarget();
        ConfigTarget waiting = editableTarget();
        when(locked.editable()).thenReturn(false);
        when(waiting.ready()).thenReturn(false);
        ConfigDefaults defaults = new ConfigDefaults();
        defaults.add(client, value);
        defaults.add(locked, value);
        defaults.add(waiting, value);

        defaults.restore();

        verify(client).setWhileDragging(value, false);
        verify(client).flush();
        verify(locked, never()).setWhileDragging(any(), any());
        verify(locked, never()).flush();
        verify(waiting, never()).setWhileDragging(any(), any());
        verify(waiting, never()).flush();
    }

    @Test
    void rechecksPermissionsAfterConfirmation()
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        var value = builder.define("value", true);
        builder.build();
        ConfigTarget target = editableTarget();
        ConfigDefaults defaults = new ConfigDefaults();
        defaults.add(target, value);
        assertTrue(defaults.canRestore());

        when(target.editable()).thenReturn(false);
        assertFalse(defaults.canRestore());
        defaults.restore();

        verify(target, never()).setWhileDragging(any(), any());
        verify(target, never()).flush();
    }

    @Test
    void rebuildingTheScreenDropsPreviouslyDisplayedSettings()
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        var value = builder.define("value", true);
        builder.build();
        ConfigTarget target = editableTarget();
        ConfigDefaults defaults = new ConfigDefaults();
        defaults.add(target, value);

        defaults.clear();
        assertFalse(defaults.canRestore());
        defaults.restore();

        verify(target, never()).setWhileDragging(any(), any());
        verify(target, never()).flush();
    }

    @Test
    void renderingPresetControlsRestoreTheirUnderlyingConfigDefaults()
    {
        ConfigTarget target = editableTarget();
        ConfigDefaults defaults = new ConfigDefaults();
        for (var value : ModClientConfig.renderPresetConfigValues())
            defaults.add(target, value);

        defaults.restore();

        for (var value : ModClientConfig.renderPresetConfigValues())
            verify(target).setWhileDragging(value, value.getDefault());
        verify(target, never()).setWhileDragging(eq(ModClientConfig.GPU_MODEL_CACHE_MEGABYTES), any());
        verify(target).flush();
    }

    private static ConfigTarget editableTarget()
    {
        ConfigTarget target = mock(ConfigTarget.class);
        when(target.ready()).thenReturn(true);
        when(target.editable()).thenReturn(true);
        return target;
    }
}
