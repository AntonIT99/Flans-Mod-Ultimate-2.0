package com.flansmodultimate.client.particle;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.AABB;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmokeGrenadeParticleTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void explosionVelocityCannotThrowSmokeAwayFromItsOriginOrSpawnDescendants()
    {
        SmokeGrenadeParticle.Provider provider = new SmokeGrenadeParticle.Provider(mock(SpriteSet.class));
        Particle particle = provider.createParticle(new SimpleParticleType(false), mock(ClientLevel.class), 12D, 64D, -8D, 10D, 5D, -10D);
        AABB origin = particle.getBoundingBox();
        try (var children = mockStatic(ParticleHelper.class))
        {
            for (int tick = 0; tick < 40; tick++)
            {
                particle.tick();
                assertEquals(origin, particle.getBoundingBox());
            }
            particle.tick();
            assertFalse(particle.isAlive());
            children.verifyNoInteractions();
        }
    }
}
