package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class AfterburnParticle extends ParticleBase
{
    protected AfterburnParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        lifetime = 6;
        gravity = 1.0F;
        xd = vx;
        yd = vy;
        zd = vz;
        quadSize = 0.8F;
        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 1.0F;
        alpha = 1.0F;
        setSpriteFromAge(sprites);
    }

    @Override
    protected void updateVisuals()
    {
        quadSize = scaleMultiplier * (0.8F - age * 0.13F);
        float c = Mth.clamp(1.0F - age * 0.2F, 0.0F, 1.0F);
        rCol = c;
        gCol = c;
        bCol = 1.0F;
        alpha = Mth.clamp(1.0F - age * 0.1F, 0.0F, 1.0F);
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
