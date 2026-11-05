package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RandomGrenadeLaunchTest
{
    @Test
    void seededLaunchesAreRepeatableAndBoundedByTheLegacyScatterPattern()
    {
        RandomSource first = RandomSource.create(1234L);
        RandomSource second = RandomSource.create(1234L);
        RandomGrenadeLaunch previous = null;
        boolean varied = false;
        for (int i = 0; i < 100; i++)
        {
            RandomGrenadeLaunch launch = RandomGrenadeLaunch.sample(first);
            assertEquals(launch, RandomGrenadeLaunch.sample(second));
            assertTrue(launch.yaw() >= 0F && launch.yaw() < 360F);
            assertTrue(launch.pitch() <= -66F && launch.pitch() >= -88F);
            assertEquals(90F, launch.roll());
            assertTrue(launch.speedScale() >= 0F && launch.speedScale() < 1.5F);
            varied |= previous != null && !previous.equals(launch);
            previous = launch;
        }
        assertTrue(varied);
    }

    @Test
    void scatterIsOptInAndPreservesTheAuthoredSpinChoice()
    {
        GrenadeType type = new GrenadeType();
        assertFalse(type.isRandomDirection());
        type.load(new TypeFile("debris", EnumType.GRENADE, new ContentPack("test", Path.of("build", "test-packs", "scatter")),
            List.of("RandomDirection true", "SpinWhenThrown false")));
        assertTrue(type.isRandomDirection());
        assertFalse(type.isSpinWhenThrown());
    }
}
