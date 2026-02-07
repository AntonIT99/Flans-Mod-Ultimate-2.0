package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class RocketExhaustParticle extends ParticleBase
{
    protected RocketExhaustParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        
        lifetime = 16;
        
        gravity = 0.0F;
        
        xd = vx;
        yd = vy;
        zd = vz;
        
        quadSize = 10.0F;
        
        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 1.0F;
        alpha = 1.0F;
        
        setSpriteFromAge(sprites);
    }

    @Override
    protected void updateVisuals()
    {
        quadSize = scaleMultiplier * (10.0F + age * 0.05F);
        
        alpha = Mth.clamp(1.0F - age * 0.1F, 0.0F, 1.0F);
        
        yd += 0.01F;
        
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
