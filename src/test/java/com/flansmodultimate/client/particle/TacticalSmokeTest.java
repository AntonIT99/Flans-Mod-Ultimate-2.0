package com.flansmodultimate.client.particle;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class TacticalSmokeTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    @SuppressWarnings("java:S3011") // Inject mocks into Minecraft final fields without starting a live client.
    void tacticalSmokeSurvivesDisabledCosmeticParticlesButKeepsAFixedBudgetAndRange() throws ReflectiveOperationException
    {
        Minecraft minecraft = mock(Minecraft.class);
        ClientLevel level = mock(ClientLevel.class);
        ParticleEngine engine = mock(ParticleEngine.class);
        GameRenderer renderer = mock(GameRenderer.class);
        Camera camera = mock(Camera.class);
        Minecraft.class.getField("level").set(minecraft, level);
        var engineField = Minecraft.class.getField("particleEngine");
        engineField.setAccessible(true);
        engineField.set(minecraft, engine);
        var rendererField = Minecraft.class.getField("gameRenderer");
        rendererField.setAccessible(true);
        rendererField.set(minecraft, renderer);
        when(renderer.getMainCamera()).thenReturn(camera);
        when(camera.getPosition()).thenReturn(Vec3.ZERO);
        Particle puff = mock(Particle.class);
        ModClientConfig preferences = mock(ModClientConfig.class);
        when(engine.createParticle(eq(ParticleTypes.SMOKE), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(puff);
        try (var game = mockStatic(Minecraft.class); var client = mockStatic(ModClientConfig.class); var common = mockStatic(ModCommonConfig.class))
        {
            game.when(Minecraft::getInstance).thenReturn(minecraft);
            // Zero-initialized client preferences remove decorative particles even nearby.
            client.when(ModClientConfig::get).thenReturn(preferences);
            common.when(ModCommonConfig::smokeParticlesRange).thenReturn(32D);
            assertNull(ParticleHelper.createFromString(FlanParticles.SMOKE, 1D, 0D, 0D, 0D, 0D, 0D, 1F, 1F));
            for (int i = 0; i < 128; i++)
                assertSame(puff, ParticleHelper.createTacticalSmoke(FlanParticles.SMOKE, 64D, 0D, 0D, 0D, 0D, 0D, 1F));
            assertNull(ParticleHelper.createTacticalSmoke(FlanParticles.SMOKE, 64D, 0D, 0D, 0D, 0D, 0D, 1F));
            when(level.getGameTime()).thenReturn(1L);
            assertSame(puff, ParticleHelper.createTacticalSmoke(FlanParticles.SMOKE, 64D, 0D, 0D, 0D, 0D, 0D, 1F));
            assertNull(ParticleHelper.createTacticalSmoke(FlanParticles.SMOKE, 129D, 0D, 0D, 0D, 0D, 0D, 1F));
        }
    }
}
