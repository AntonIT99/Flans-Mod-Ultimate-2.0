package com.flansmodultimate.config;

import io.netty.buffer.Unpooled;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The 1.7.10 gameplay settings restored in the common and client configs. */
class LegacyGameplayConfigTest
{
    @Test
    void commonConfigDeclaresTheLegacySettingsWithTheirDefaults()
    {
        assertSetting(ModCommonConfig.configSpec, 20D, "General Settings", "maxPlayerHealth");
        assertSetting(ModCommonConfig.configSpec, true, "General Settings", "enableKillMessages");
        assertSetting(ModCommonConfig.configSpec, true, "General Settings", "showDistanceInKillMessage");
        assertSetting(ModCommonConfig.configSpec, false, "Gun Settings", "gunDevMode");
        assertSetting(ModCommonConfig.configSpec, 0, "Teams Settings", "bulletSnapshotMin");
        assertSetting(ModCommonConfig.configSpec, 50, "Teams Settings", "bulletSnapshotDivisor");

        ForgeConfigSpec.ValueSpec min = ModCommonConfig.configSpec.get(List.of("Teams Settings", "bulletSnapshotMin"));
        assertTrue(min.test(100) && !min.test(101), "the legacy bltss command accepted 0 to 100");
        ForgeConfigSpec.ValueSpec divisor = ModCommonConfig.configSpec.get(List.of("Teams Settings", "bulletSnapshotDivisor"));
        assertTrue(divisor.test(1000) && !divisor.test(1001), "the legacy bltss command accepted 0 to 1000");
    }

    @Test
    void clientConfigDeclaresTheHitMarkerAndTooltipSettings()
    {
        assertSetting(ModClientConfig.configSpec, true, "General Settings", "showDetailedItemDescriptions");
        assertSetting(ModClientConfig.configSpec, true, "General Settings", "showHitMarker");
        for (String channel : List.of("hitMarkerRed", "hitMarkerGreen", "hitMarkerBlue", "hitMarkerAlpha"))
            assertSetting(ModClientConfig.configSpec, 1D, "General Settings", channel);
    }

    /** Every field must travel to the client in the order it is read back, or the snapshot shifts. */
    @Test
    void commonSnapshotSurvivesTheNetworkRoundTrip() throws ReflectiveOperationException
    {
        RecordComponent[] components = CommonConfigSnapshot.class.getRecordComponents();
        Object[] values = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++)
        {
            types[i] = components[i].getType();
            values[i] = sample(types[i], i);
        }
        values[0] = CommonConfigSnapshot.CURRENT_VERSION;
        Constructor<CommonConfigSnapshot> constructor = CommonConfigSnapshot.class.getDeclaredConstructor(types);
        CommonConfigSnapshot snapshot = constructor.newInstance(values);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        CommonConfigSnapshot.write(buffer, snapshot);
        assertEquals(snapshot, CommonConfigSnapshot.read(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    /** A value of the component's type that differs from its neighbours, so a shifted field is noticed. */
    private static Object sample(Class<?> type, int index)
    {
        if (type == int.class)
            return index + 1;
        if (type == float.class)
            return index + 0.5F;
        if (type == double.class)
            return index + 0.25D;
        if (type == boolean.class)
            return index % 2 == 0;
        if (type == String.class)
            return "value" + index;
        if (type == List.class)
            return List.of("line" + index);
        if (type.isEnum())
            return type.getEnumConstants()[index % type.getEnumConstants().length];
        throw new IllegalArgumentException("No sample for " + type);
    }

    private static void assertSetting(ForgeConfigSpec spec, Object expectedDefault, String... path)
    {
        ForgeConfigSpec.ValueSpec value = spec.get(List.of(path));
        assertNotNull(value, String.join(".", path) + " is missing");
        assertEquals(expectedDefault, value.getDefault(), String.join(".", path));
    }
}
