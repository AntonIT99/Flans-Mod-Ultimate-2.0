package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.config.ModClientConfig;
import net.neoforged.neoforge.common.ModConfigSpec.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigDefaultsTest
{
    @Test
    void uncensoredPreferenceSurvivesRestoreDefaults()
    {
        ConfigTarget target = editableTarget();
        ConfigDefaults defaults = new ConfigDefaults();
        defaults.add(target, ModClientConfig.ENABLE_UNCENSORED_CONTENT);
        assertEquals(0, defaults.changedCount());
        defaults.restore();
        verify(target, never()).setWhileDragging(any(), any());
        verify(target, never()).flush();
    }

    @Test
    void countsOnlyDistinctEditableDisplayedValuesThatDifferFromDefaults()
    {
        Builder builder = new Builder();
        var changed = builder.define("changed", true);
        var unchanged = builder.defineInRange("unchanged", 0.25D, 0D, 1D);
        var hidden = builder.define("hidden", true);
        builder.build();
        ConfigTarget target = editableTarget();
        ConfigTarget locked = editableTarget();
        ConfigTarget waiting = editableTarget();
        when(target.get(changed)).thenReturn(false);
        when(target.get(unchanged)).thenReturn(0.25D);
        when(locked.editable()).thenReturn(false);
        when(waiting.ready()).thenReturn(false);
        ConfigDefaults defaults = new ConfigDefaults();
        defaults.add(target, changed);
        defaults.add(target, unchanged);
        defaults.add(target, changed);
        defaults.add(locked, hidden);
        defaults.add(waiting, hidden);

        assertEquals(1, defaults.changedCount());
        assertEquals(List.of(changed), defaults.changedValues());
        when(target.get(changed)).thenReturn(true);
        assertEquals(0, defaults.changedCount());
        assertTrue(defaults.changedValues().isEmpty());
        assertFalse(defaults.canRestore());
    }

    @Test
    void leavesAlreadyDefaultValuesUntouchedAndDoesNotFlushANoOpReset()
    {
        Builder builder = new Builder();
        var changed = builder.define("changed", true);
        var unchanged = builder.defineInRange("unchanged", 5, 0, 10);
        builder.build();
        ConfigTarget target = editableTarget();
        when(target.get(changed)).thenReturn(false);
        when(target.get(unchanged)).thenReturn(5);
        ConfigDefaults defaults = new ConfigDefaults();
        defaults.add(target, changed);
        defaults.add(target, unchanged);

        defaults.restore();

        verify(target).setWhileDragging(changed, true);
        verify(target, never()).setWhileDragging(eq(unchanged), any());
        verify(target).flush();
        when(target.get(changed)).thenReturn(true);
        defaults.restore();
        verify(target, times(1)).setWhileDragging(changed, true);
        verify(target, times(1)).flush();
    }

    @Test
    void restoresOnlyDisplayedValuesAndFlushesEachTargetOnce()
    {
        Builder builder = new Builder();
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
        Builder builder = new Builder();
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
        Builder builder = new Builder();
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
        Builder builder = new Builder();
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
