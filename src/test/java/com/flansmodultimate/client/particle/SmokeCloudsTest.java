package com.flansmodultimate.client.particle;

import org.junit.jupiter.api.Test;

import net.minecraft.client.particle.Particle;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmokeCloudsTest
{
    @Test
    void oneSourceCannotSpendTheGlobalBudgetOrCatchUpWithABurstOfPackets()
    {
        SmokeClouds.Cloud cloud = new SmokeClouds.Cloud();
        assertEquals(2, cloud.request(100, 100000));
        for (int packet = 0; packet < 100; packet++)
            assertEquals(0, cloud.request(100, 100000));
        assertEquals(2, cloud.request(101, 100000));
        assertEquals(2, new SmokeClouds.Cloud().request(100, 100000), "another grenade has its own allowance");
    }

    @Test
    void lowCountsAndDisabledEffectsArePreserved()
    {
        SmokeClouds.Cloud cloud = new SmokeClouds.Cloud();
        assertEquals(0, cloud.request(0, 0));
        assertEquals(0, cloud.request(1, -10));
        assertEquals(1, cloud.request(2, 1));
        assertEquals(0, cloud.request(2, 1));
        assertEquals(1, cloud.request(3, 1));
    }

    @Test
    void liveParticlesStayBoundedUntilOldPuffsDie()
    {
        SmokeClouds.Cloud cloud = new SmokeClouds.Cloud();
        List<Particle> particles = new ArrayList<>();
        for (int tick = 0; tick < 32; tick++)
        {
            int count = cloud.request(tick, 50);
            assertEquals(2, count);
            for (int i = 0; i < count; i++)
            {
                Particle particle = mock(Particle.class);
                when(particle.isAlive()).thenReturn(true);
                when(particle.getLifetime()).thenReturn(300);
                cloud.track(particle);
                verify(particle).setLifetime(40);
                particles.add(particle);
            }
        }
        assertEquals(0, cloud.request(32, 50));
        when(particles.get(0).isAlive()).thenReturn(false);
        assertEquals(1, cloud.request(33, 50));
        assertFalse(cloud.isFinished(34));
        particles.forEach(particle -> when(particle.isAlive()).thenReturn(false));
        assertTrue(cloud.isFinished(100));
    }

    @Test
    void engineEvictionOrResourceReloadCannotLeaveASourcePermanentlyFull()
    {
        SmokeClouds.Cloud cloud = new SmokeClouds.Cloud();
        cloud.request(0, 50);
        Particle particle = mock(Particle.class);
        when(particle.isAlive()).thenReturn(true);
        when(particle.getLifetime()).thenReturn(300);
        cloud.track(particle);
        assertFalse(cloud.isFinished(40));
        assertTrue(cloud.isFinished(41), "the allowance expires even when the engine dropped the particle without removing it");
    }

    @Test
    void smokeControllersCannotSpawnUntrackedDescendants()
    {
        for (String type : List.of("flansmod.Smoker1", "Smoker", "bigsmoke", "hugeexplosion", "flare", "fmflame", "debris1", "fmtracer", "fmtracergreen", "fmtracerred"))
            assertEquals("flansmod.smoker", SmokeClouds.visibleParticle(type), type);
        assertEquals("cloud", SmokeClouds.visibleParticle("cloud"));
        assertEquals("explode", SmokeClouds.visibleParticle("explode"));
    }
}
