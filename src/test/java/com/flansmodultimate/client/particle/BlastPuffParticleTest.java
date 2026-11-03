package com.flansmodultimate.client.particle;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlastPuffParticleTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void scorchExpiresWithTheWrecksRemainingLifetime()
    {
        BlastPuffParticle particle = scorch(mock(Entity.class), 75);
        for (int i = 0; i < 75; i++)
            particle.tick();
        assertTrue(particle.isAlive());
        particle.tick();
        assertFalse(particle.isAlive());
    }

    @ParameterizedTest
    @EnumSource(Entity.RemovalReason.class)
    void everyClientRemovalFadesTheScorchWithinTwoSeconds(Entity.RemovalReason reason)
    {
        Entity owner = mock(Entity.class);
        BlastPuffParticle particle = scorch(owner, 6000);
        particle.tick();
        when(owner.isRemoved()).thenReturn(true);
        when(owner.getRemovalReason()).thenReturn(reason);
        for (int i = 0; i < 39; i++)
            particle.tick();
        assertTrue(particle.isAlive());
        particle.tick();
        assertFalse(particle.isAlive());
    }

    @Test
    void unownedScorchKeepsItsOwnLifetime()
    {
        BlastPuffParticle particle = scorch(null, 2400);
        for (int i = 0; i < 100; i++)
            particle.tick();
        assertTrue(particle.isAlive());
    }

    private static BlastPuffParticle scorch(Entity owner, int lifetime)
    {
        SpriteSet sprites = mock(SpriteSet.class);
        TextureAtlasSprite sprite = mock(TextureAtlasSprite.class);
        when(sprites.get(any(RandomSource.class))).thenReturn(sprite);
        new BlastPuffParticle.Provider(sprites);
        BlastPuffParticle.Look look = new BlastPuffParticle.Look(0F, 0F, 0F, 0F, 0F, 0F, 0.8F, 0F, 1F, 0F, 0F, false, false);
        ClientLevel level = mock(ClientLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        BlastPuffParticle particle = BlastPuffParticle.createFlat(level, 0D, 0D, 0D, look, 2F, lifetime, owner);
        assertNotNull(particle);
        return particle;
    }
}
