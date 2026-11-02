package com.flansmodultimate.client.particle;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyEffectParticleTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void columnControllersHaveALiveCapAndReleaseItWhenRemovedOrChangingWorlds()
    {
        ClientLevel level = mock(ClientLevel.class);
        LegacyEffectParticle.useLevel(null);
        Particle first = particle(level, LegacyEffectParticle.Style.TANK_DEATH);
        assertNotNull(first);
        java.util.List<Particle> live = new java.util.ArrayList<>();
        live.add(first);
        for (int i = 1; i < 16; i++)
        {
            Particle next = particle(level, LegacyEffectParticle.Style.TANK_DEATH);
            assertNotNull(next);
            live.add(next);
        }
        assertNull(particle(level, LegacyEffectParticle.Style.TANK_DEATH));
        assertTrue(live.stream().allMatch(Particle::isAlive));
        first.remove();
        assertNotNull(particle(level, LegacyEffectParticle.Style.TANK_DEATH));
        assertNotNull(particle(mock(ClientLevel.class), LegacyEffectParticle.Style.TANK_DEATH));
    }

    @Test
    void casingsRenderAsNarrowCasesAndScaleWithTheirWeapon()
    {
        Particle casing = particle(mock(ClientLevel.class), LegacyEffectParticle.Style.CASING);
        assertNotNull(casing);
        casing.scale(2F);
        VertexConsumer vertices = mock(VertexConsumer.class, RETURNS_SELF);
        Camera camera = mock(Camera.class);
        when(camera.getPosition()).thenReturn(Vec3.ZERO);
        when(camera.rotation()).thenReturn(new Quaternionf());
        casing.render(vertices, camera, 0F);
        ArgumentCaptor<Double> x = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> y = ArgumentCaptor.forClass(Double.class);
        verify(vertices, times(4)).vertex(x.capture(), y.capture(), anyDouble());
        double height = Math.hypot(x.getAllValues().get(1) - x.getAllValues().get(0), y.getAllValues().get(1) - y.getAllValues().get(0));
        double width = Math.hypot(x.getAllValues().get(2) - x.getAllValues().get(1), y.getAllValues().get(2) - y.getAllValues().get(1));
        assertEquals(0.24D, height, 0.00001D);
        assertEquals(0.35D, width / height, 0.00001D);
    }

    @Test
    void gasAndWaterStayColdWhileMuzzleSparksAreHot()
    {
        for (LegacyEffectParticle.Style style : new LegacyEffectParticle.Style[]{LegacyEffectParticle.Style.MUSTARD, LegacyEffectParticle.Style.CHLORINE, LegacyEffectParticle.Style.WATER,
            LegacyEffectParticle.Style.FLASHER, LegacyEffectParticle.Style.CASING})
            assertFalse(((LegacyEffectParticle) particle(mock(ClientLevel.class), style)).isHot());
        assertTrue(((LegacyEffectParticle) particle(mock(ClientLevel.class), LegacyEffectParticle.Style.NU_SPARK)).isHot());
    }

    private static Particle particle(ClientLevel level, LegacyEffectParticle.Style style)
    {
        SpriteSet sprites = mock(SpriteSet.class);
        TextureAtlasSprite sprite = mock(TextureAtlasSprite.class);
        when(sprites.get(any(RandomSource.class))).thenReturn(sprite);
        return new LegacyEffectParticle.Provider(sprites, style).createParticle(ParticleTypes.FLAME, level, 0D, 0D, 0D, 0D, 0D, 0D);
    }
}
