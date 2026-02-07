package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class FmMuzzleFlashParticle extends ParticleBase
{
    protected FmMuzzleFlashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        
        lifetime = 4;

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
    public void tick()
    {
        xo = x;
        yo = y;
        zo = z;
        
        xd += random.nextGaussian() * 0.005D;
        yd += random.nextGaussian() * 0.005D;
        zd += random.nextGaussian() * 0.005D;
        
        move(xd, yd, zd);
        
        xd *= 0.5D;
        yd *= 0.1D;
        zd *= 0.5D;

        if (onGround || age++ >= lifetime)
        {
            remove();
            return;
        }

        updateVisuals();
    }

    @Override
    protected void updateVisuals()
    {
        quadSize = scaleMultiplier * 0.1F;
        alpha = 1.0F;
        
        double progress = (double)age / (double)lifetime;

        int frameIndex;
        if (progress < 0.3D)
            frameIndex = 0;
        else if (progress < 0.6D)
            frameIndex = 1;
        else if (progress < 0.8D)
            frameIndex = 2;
        else
            frameIndex = 3;
        
        setSprite(sprites.get(frameIndex, 4));
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