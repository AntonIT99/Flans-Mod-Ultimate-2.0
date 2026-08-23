package com.flansmodultimate.client.particle;
import com.flansmodultimate.FlansMod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class FlareParticle extends ParticleBase
{
    protected FlareParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        
        lifetime *= 100;

        gravity = 1.0F;

        xd = vx;
        yd = vy;
        zd = vz;
        
        quadSize *= 3.0F;
        
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

        if (age++ >= lifetime)
            remove();
        
        yd -= 0.04D * gravity;
        
        move(xd, yd, zd);
        
        xd *= 0.99D;
        yd *= 0.99D;
        zd *= 0.99D;
        
        if (y < 0.0D)
            remove();
        
        final int NUM = 5;
        double dx = (x - xo) / NUM;
        double dy = (y - yo) / NUM;
        double dz = (z - zo) / NUM;

        for (int i = 0; i < NUM; ++i)
        {
            double px = xo + dx * i;
            double py = yo + dy * i;
            double pz = zo + dz * i;
            
            level.addParticle(FlansMod.fmFlameParticle.get(), px, py, pz, 0.0D, 0.0D, 0.0D);
        }

        if (onGround)
            remove();

        updateVisuals();
    }

    @Override
    protected void updateVisuals()
    {
        alpha = 1.0F;

        setSpriteFromAge(sprites);
    }

    @Override
    public Particle scale(float factor)
    {
        return applyScale(factor);
    }

    @Override
    @NotNull
    public ParticleRenderType getGroup()
    {
        // The flare acts only as the moving controller for its fmflame trail.
        // Its own flare.png quad is intentionally not submitted for rendering.
        return ParticleRenderType.NO_RENDER;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz, RandomSource random)
        {
            return new FlareParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
