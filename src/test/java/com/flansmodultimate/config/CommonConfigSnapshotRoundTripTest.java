package com.flansmodultimate.config;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonConfigSnapshotRoundTripTest
{
    @Test
    void suppressionOptionsSurviveSynchronizationWithTheRestOfTheServerConfig() throws ReflectiveOperationException
    {
        RecordComponent[] components = CommonConfigSnapshot.class.getRecordComponents();
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++)
            values[i] = sample(components[i]);
        Class<?>[] signature = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        CommonConfigSnapshot original = CommonConfigSnapshot.class.getDeclaredConstructor(signature).newInstance(values);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            CommonConfigSnapshot.write(buffer, original);
            CommonConfigSnapshot decoded = CommonConfigSnapshot.read(buffer);
            assertEquals(original, decoded);
            assertTrue(decoded.nearMissSuppression());
            assertTrue(decoded.suppressionAffectsRiders());
            assertEquals(1.25D, decoded.suppressionIntensity());
            assertEquals(1.25D, decoded.suppressionRangeMultiplier());
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
    }

    private static Object sample(RecordComponent component)
    {
        Class<?> type = component.getType();
        if (component.getName().equals("version"))
            return CommonConfigSnapshot.CURRENT_VERSION;
        if (type == boolean.class)
            return true;
        if (type == int.class)
            return 3;
        if (type == float.class)
            return 1.25F;
        if (type == double.class)
            return 1.25D;
        if (type == String.class)
            return "sample";
        if (type == List.class)
            return List.of("sample");
        if (type.isEnum())
            return type.getEnumConstants()[0];
        throw new IllegalStateException("No sample for " + component);
    }
}
