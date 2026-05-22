package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class SmokeBurstParticle extends ParticleBase 
{
    protected SmokeBurstParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) 
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        lifetime = 6;
        gravity = 0.0F;
        quadSize = 5.0F;
        rCol = gCol = bCol = 1.0F;
        setSpriteFromAge(sprites);
    }

    @Override
    protected void updateVisuals()
    {
        setSpriteFromAge(sprites);
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
        {
            return new SmokeBurstParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
