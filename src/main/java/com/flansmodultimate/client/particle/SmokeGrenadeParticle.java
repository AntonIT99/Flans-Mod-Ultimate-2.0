package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansModParticles;

import com.flansmodultimate.common.physics.ModPhysics;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;

public class SmokeGrenadeParticle extends ParticleBase
{
    private int dischargeTime;

    protected SmokeGrenadeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        
        lifetime *= 20;
        
        gravity = 1.0F;
        
        xd = vx;
        yd = vy;
        zd = vz;
        
        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 1.0F;
        alpha = 1.0F;
        
        dischargeTime = 20;
        
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
        
        yd -= ModPhysics.gravity(0.04D * gravity, level);
        
        move(xd, yd, zd);
        
        xd *= ModPhysics.dragRetention(0.99D, level);
        yd *= ModPhysics.dragRetention(0.99D, level);
        zd *= ModPhysics.dragRetention(0.99D, level);
        
        if (isInLiquid())
            yd = 1.0D;
        
        dischargeTime--;

        if (dischargeTime < 0)
        {
            ParticleHelper.spawnSubParticle(FlansModParticles.smokeBurstParticle.get(), x, y, z);
            ParticleHelper.spawnSubParticle(FlansModParticles.bigSmokeParticle.get(), x, y, z);
            remove();
        }
        
        final int NUM = 5;
        double dx = (x - xo) / NUM;
        double dy = (y - yo) / NUM;
        double dz = (z - zo) / NUM;

        for (int i = 0; i < NUM; i++)
        {
            double px = xo + dx * i;
            double py = yo + dy * i;
            double pz = zo + dz * i;

            ParticleHelper.spawnSubParticle(FlansModParticles.explodeParticle.get(), px, py, pz);
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
    @NotNull
    public Particle scale(float factor)
    {
        return applyScale(factor);
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return LegacyParticleRenderTypes.PREMULTIPLIED;
    }

    private boolean isInLiquid()
    {
        BlockPos pos = BlockPos.containing(x, y, z);
        return !level.getFluidState(pos).isEmpty();
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
        {
            return new SmokeGrenadeParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
