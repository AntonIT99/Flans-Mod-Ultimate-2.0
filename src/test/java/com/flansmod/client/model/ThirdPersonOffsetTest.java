package com.flansmod.client.model;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.GunAnimationConfig;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ThirdPersonOffsetTest
{
    @Test
    void textOffsetAddsToJavaModelAndDoesNotAccumulateWhenTypeIsReapplied()
    {
        ModelGun model = new ModelGun();
        Vector3f javaOffset = new Vector3f(1F, 2F, 3F);
        model.setThirdPersonOffset(javaOffset);
        GunAnimationConfig config = new GunAnimationConfig();
        config.read(new TypeFile("offset", EnumType.GUN, null, List.of("animThirdPersonOffset -0.5 0.25 2")));
        GunType type = mock(GunType.class);
        when(type.getAnimationConfig()).thenReturn(config);

        model.setType(type);
        assertOffset(model.getThirdPersonOffset(), 0.5F, 2.25F, 5F);
        model.setType(type);
        assertOffset(model.getThirdPersonOffset(), 0.5F, 2.25F, 5F);
        assertOffset(javaOffset, 1F, 2F, 3F);
        assertOffset(config.getThirdPersonOffset(), -0.5F, 0.25F, 2F);

        when(type.getAnimationConfig()).thenReturn(new GunAnimationConfig());
        model.setType(type);
        assertOffset(model.getThirdPersonOffset(), 1F, 2F, 3F);
    }

    @Test
    void unconfiguredModelKeepsJavaOffset()
    {
        ModelGun model = new ModelGun();
        model.setThirdPersonOffset(new Vector3f(1F, 2F, 3F));
        assertOffset(model.getThirdPersonOffset(), 1F, 2F, 3F);
    }

    private static void assertOffset(Vector3f offset, float x, float y, float z)
    {
        assertEquals(x, offset.x);
        assertEquals(y, offset.y);
        assertEquals(z, offset.z);
    }
}
