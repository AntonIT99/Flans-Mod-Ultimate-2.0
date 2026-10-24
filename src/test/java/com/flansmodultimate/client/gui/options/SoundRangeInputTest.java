package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.config.ConfigSpecValues;
import com.flansmodultimate.config.ModClientConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoundRangeInputTest
{
    @Test
    void acceptsDecimalsAndTheFullConfiguredRange()
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.DoubleValue range = builder.defineInRange("range", 64D, 1D, 4096D);
        ForgeConfigSpec.ValueSpec spec = ConfigSpecValues.valueSpec(builder.build(), range.getPath());

        assertEquals(64.25D, SoundRangeInput.parse("64.25", spec).orElseThrow());
        assertEquals(1D, SoundRangeInput.parse("1", spec).orElseThrow());
        assertEquals(4096D, SoundRangeInput.parse("4096", spec).orElseThrow());
        for (String invalid : new String[]{"", "-", "text", "0", "4096.01", "NaN", "Infinity", "1e309"})
            assertTrue(SoundRangeInput.parse(invalid, spec).isEmpty(), invalid);
    }

    @Test
    void keepsFractionalRangesInTheDisplayedValue()
    {
        assertEquals("64", SoundRangeInput.format(64D));
        assertEquals("64.25", SoundRangeInput.format(64.25D));
    }

    @Test
    void renderingDefaultsMatchTheirConfigValues()
    {
        assertEquals(ModClientConfig.RenderPreset.QUALITY, ModClientConfig.defaultLodPreset());
        assertEquals(ModClientConfig.RenderPreset.QUALITY, ModClientConfig.defaultImpostorPreset());
    }
}
