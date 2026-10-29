package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.config.ConfigSpecValues;
import com.flansmodultimate.config.ModClientConfig;
import net.neoforged.neoforge.common.ModConfigSpec.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NumericConfigInputTest
{
    @Test
    void allSettingsUsesInputsForWideRangesWhileQuickOptionsKeepTheirSliders()
    {
        Builder builder = new Builder().push("Settings");
        IntValue wideCount = builder.defineInRange("count", 100, 0, Integer.MAX_VALUE);
        builder.defineInRange("smallCount", 5, 1, 20);
        builder.defineInRange("pitch", 0.5D, 0D, Float.MAX_VALUE);
        builder.defineInRange("opacity", 1D, 0D, 1D);
        ConfigTarget target = mock(ConfigTarget.class);
        when(target.spec()).thenReturn(builder.pop().build());
        when(target.get(any())).thenAnswer(call -> ((ConfigValue<?>) call.getArgument(0)).getDefault());
        when(target.editable()).thenReturn(true);

        var options = ConfigOptionFactory.sections(target).get(0).options();
        assertNotNull(options.get(0).label());
        assertNull(options.get(1).label());
        assertNotNull(options.get(2).label());
        assertNull(options.get(3).label());
        assertNull(ConfigOptionFactory.option(target, wideCount).label());
    }

    @Test
    void acceptsDecimalsAndTheFullConfiguredRange()
    {
        Builder builder = new Builder();
        DoubleValue range = builder.defineInRange("range", 64D, 1D, 4096D);
        ValueSpec spec = ConfigSpecValues.valueSpec(builder.build(), range.getPath());

        assertEquals(64.25D, NumericConfigInput.parse("64.25", spec, Double::valueOf).orElseThrow());
        assertEquals(1D, NumericConfigInput.parse("1", spec, Double::valueOf).orElseThrow());
        assertEquals(4096D, NumericConfigInput.parse("4096", spec, Double::valueOf).orElseThrow());
        for (String invalid : new String[]{"", "-", "text", "0", "4096.01", "NaN", "Infinity", "1e309"})
            assertTrue(NumericConfigInput.parse(invalid, spec, Double::valueOf).isEmpty(), invalid);
    }

    @Test
    void acceptsFullIntegerBoundsWithoutRoundingOrOverflow()
    {
        Builder builder = new Builder();
        IntValue count = builder.defineInRange("count", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        ValueSpec spec = ConfigSpecValues.valueSpec(builder.build(), count.getPath());

        assertEquals(Integer.MAX_VALUE, NumericConfigInput.parse("2147483647", spec, Integer::valueOf).orElseThrow());
        assertEquals(Integer.MIN_VALUE, NumericConfigInput.parse("-2147483648", spec, Integer::valueOf).orElseThrow());
        for (String invalid : new String[]{"1.5", "2147483648", "-2147483649", "NaN", "Infinity"})
            assertTrue(NumericConfigInput.parse(invalid, spec, Integer::valueOf).isEmpty(), invalid);
    }

    @Test
    void keepsFractionalRangesInTheDisplayedValue()
    {
        assertEquals("64", NumericConfigInput.format(64D));
        assertEquals("64.25", NumericConfigInput.format(64.25D));
    }

    @Test
    void renderingDefaultsMatchTheirConfigValues()
    {
        assertEquals(ModClientConfig.RenderPreset.QUALITY, ModClientConfig.defaultLodPreset());
        assertEquals(ModClientConfig.RenderPreset.QUALITY, ModClientConfig.defaultImpostorPreset());
    }
}
