package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class FmMuzzleFlashParticle extends ParticleBase
{
    protected FmMuzzleFlashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        
        lifetime = (int)(lifetime * 0.25D);

        gravity = 0.0F;

        xd = vx;
        yd = vy;
        zd = vz;

        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 1.0F;
        alpha = 1.0F;
        
        updateVisuals();
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
        }

        updateVisuals();
    }

    @Override
    protected void updateVisuals()
    {
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
        
        setSprite(sprites.get(frameIndex, 3));
    }

    @Override
    public Particle scale(float factor)
    {
        // ClientProxy applied multipleParticleScaleBy once in the muzzle-flash
        // branch and once again in its common tail. particleScale was therefore
        // multiplied twice, while setSize was assigned the same value twice.
        quadSize *= factor * factor;
        setSize(0.2F * factor, 0.2F * factor);
        return this;
    }

    @Override
    public int getLightCoords(float partialTick)
    {
        return 0xF000F0;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz, RandomSource random)
        {
            return new FmMuzzleFlashParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
