package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class FlashParticle extends ParticleBase
{
    protected FlashParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);

        lifetime = 6;

        gravity = 0.0F;

        xd = vx;
        yd = vy;
        zd = vz;

        quadSize = 1.0F;
        
        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 1.0F;
        alpha = 1.0F;

        setFrame(0);
    }

    @Override
    public void tick()
    {
        xo = x;
        yo = y;
        zo = z;

        if (age++ >= lifetime)
            remove();

        if (onGround)
            remove();

        updateVisuals();
    }

    @Override
    protected void updateVisuals()
    {
        quadSize = scaleMultiplier;
        alpha = 1.0F;
        
        setFrame(Math.min(age, 5));
    }

    private void setFrame(int frame)
    {
        setSprite(sprites.get(frame, 5));
    }

    @Override
    public int getLightCoords(float partialTick)
    {
        return 0xF000F0;
    }

    @Override
    @NotNull
    protected SingleQuadParticle.Layer getLayer()
    {
        return LegacyParticleRenderTypes.PREMULTIPLIED;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz, RandomSource random)
        {
            return new FlashParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
