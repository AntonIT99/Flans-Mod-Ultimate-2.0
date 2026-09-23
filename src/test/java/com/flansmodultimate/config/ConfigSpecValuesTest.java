package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import io.netty.buffer.Unpooled;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigSpecValuesTest
{
    private enum Flavour
    {
        MILD,
        HOT
    }

    private ModConfigSpec spec;
    private ModConfigSpec.BooleanValue flag;
    private ModConfigSpec.IntValue count;
    private ModConfigSpec.DoubleValue ratio;
    private ModConfigSpec.EnumValue<Flavour> flavour;
    private ModConfigSpec.ConfigValue<String> name;

    @BeforeEach
    void buildSpec()
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("Section");
        flag = builder.define("flag", true);
        count = builder.defineInRange("count", 5, 0, 10);
        ratio = builder.defineInRange("ratio", 0.5D, 0D, 1D);
        flavour = builder.defineEnum("flavour", Flavour.MILD);
        name = builder.define("name", "unnamed");
        builder.pop();
        spec = builder.build();

        CommentedConfig config = CommentedConfig.inMemory();
        spec.correct(config);
        TestConfigLoader.load(spec, config);
    }

    @Test
    void collectsEditableEntriesByPathAndLeavesTextAlone()
    {
        Map<String, Object> values = ConfigSpecValues.collect(spec);

        assertEquals(true, values.get("Section.flag"));
        assertEquals(5, values.get("Section.count"));
        assertEquals(0.5D, values.get("Section.ratio"));
        // Enums travel as their constant name, free text does not travel at all
        assertEquals("MILD", values.get("Section.flavour"));
        assertFalse(values.containsKey("Section.name"));
        assertEquals("unnamed", name.get());
    }

    @Test
    void survivesTheRoundTripOverTheNetwork()
    {
        Map<String, Object> values = ConfigSpecValues.collect(spec);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ConfigSpecValues.write(buffer, values);

        assertEquals(values, ConfigSpecValues.read(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void appliesValuesTheSpecAccepts()
    {
        assertTrue(ConfigSpecValues.apply(spec, List.of("Section", "flag"), false));
        assertFalse(flag.get());

        assertTrue(ConfigSpecValues.apply(spec, List.of("Section", "count"), 7));
        assertEquals(7, count.get());

        assertTrue(ConfigSpecValues.apply(spec, List.of("Section", "ratio"), 0.25D));
        assertEquals(0.25D, ratio.get());

        // An enum arrives as its name and is turned back into the constant
        assertTrue(ConfigSpecValues.apply(spec, List.of("Section", "flavour"), "HOT"));
        assertEquals(Flavour.HOT, flavour.get());
    }

    @Test
    void refusesValuesTheSpecRejects()
    {
        assertFalse(ConfigSpecValues.apply(spec, List.of("Section", "count"), 99));
        assertEquals(5, count.get());

        assertFalse(ConfigSpecValues.apply(spec, List.of("Section", "flavour"), "SCORCHING"));
        assertEquals(Flavour.MILD, flavour.get());

        assertFalse(ConfigSpecValues.apply(spec, List.of("Section", "flag"), "yes please"));
        assertTrue(flag.get());

        assertFalse(ConfigSpecValues.apply(spec, List.of("Section", "nothingHere"), true));
    }

    @Test
    void reportsThatAnUnchangedValueChangedNothing()
    {
        assertFalse(ConfigSpecValues.apply(spec, List.of("Section", "count"), 5));
    }
}
