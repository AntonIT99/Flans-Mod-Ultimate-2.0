package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrownGunLifetimeTest
{
    @Test
    void worldOverrideSurvivesSavingIncludingDisabledDespawn()
    {
        ThrownGunLifetime data = new ThrownGunLifetime();
        for (int seconds : new int[]{300, 900, 0, Integer.MAX_VALUE / 20})
        {
            data.setSeconds(seconds);
            CompoundTag tag = data.save(new CompoundTag());
            assertEquals(seconds, ThrownGunLifetime.load(tag).seconds());
            assertTrue(data.isDirty());
        }
    }

    @Test
    void resetAndMissingSavedValueFollowTheConfigDefault()
    {
        ThrownGunLifetime data = ThrownGunLifetime.load(new CompoundTag());
        assertEquals(-1, data.save(new CompoundTag()).getInt("seconds"));
        data.setSeconds(120);
        data.setSeconds(-1);
        assertEquals(-1, data.save(new CompoundTag()).getInt("seconds"));
    }

    @Test
    void oversizedSavedDurationCannotOverflowTicks()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("seconds", Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE / 20, ThrownGunLifetime.load(tag).seconds());
    }
}
