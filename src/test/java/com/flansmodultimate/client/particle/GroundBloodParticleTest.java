package com.flansmodultimate.client.particle;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroundBloodParticleTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void splatterIsHorizontalAndExpiresAfterThirtyFiveSeconds()
    {
        ClientLevel level = level();
        Particle particle = particle(level);
        assertEquals(700, particle.getLifetime());
        VertexConsumer vertices = mock(VertexConsumer.class, RETURNS_SELF);
        Camera camera = mock(Camera.class);
        when(camera.getPosition()).thenReturn(Vec3.ZERO);
        particle.render(vertices, camera, 0.5F);
        ArgumentCaptor<Double> heights = ArgumentCaptor.forClass(Double.class);
        verify(vertices, times(4)).vertex(anyDouble(), heights.capture(), anyDouble());
        assertTrue(heights.getAllValues().stream().allMatch(height -> Math.abs(height - 1.003D) < 0.00001D));
        for (int tick = 0; tick < 699; tick++)
            particle.tick();
        assertTrue(particle.isAlive());
        particle.tick();
        assertFalse(particle.isAlive());
    }

    @Test
    void destroyingSupportRemovesTheSplatter()
    {
        ClientLevel level = level();
        Particle particle = particle(level);
        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.AIR.defaultBlockState());
        particle.tick();
        assertFalse(particle.isAlive());
    }

    private static ClientLevel level()
    {
        ClientLevel level = mock(ClientLevel.class);
        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.STONE.defaultBlockState());
        when(level.getFluidState(any(BlockPos.class))).thenReturn(Fluids.EMPTY.defaultFluidState());
        return level;
    }

    private static Particle particle(ClientLevel level)
    {
        SpriteSet sprites = mock(SpriteSet.class);
        TextureAtlasSprite sprite = mock(TextureAtlasSprite.class);
        when(sprites.get(any(RandomSource.class))).thenReturn(sprite);
        return new GroundBloodParticle.Provider(sprites).createParticle(ParticleTypes.FLAME, level, 0.5D, 1.003D, 0.5D, 0D, 0D, 0D);
    }
}
