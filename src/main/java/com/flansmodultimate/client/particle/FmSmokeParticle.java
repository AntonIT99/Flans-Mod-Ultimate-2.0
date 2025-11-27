package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class FmSmokeParticle extends ParticleBase
{
    protected FmSmokeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        
        lifetime = 16;
        
        gravity = 0.0F;

        xd = vx;
        yd = vy;
        zd = vz;
        
        quadSize = 0.1F;
        
        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 1.0F;
        alpha = 1.0F;

        setSpriteFromAge(sprites);
    }

    @Override
    protected void updateVisuals()
    {
        yd += 0.01F;
        
        quadSize = scaleMultiplier * (0.1F + age * 0.05F);
        
        float intensity = 0.5F;
        rCol = intensity;
        gCol = intensity;
        bCol = intensity;
        
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