package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansMod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

public class SmokeGrenadeParticle extends ParticleBase
{
    private int dischargeTime;

    protected SmokeGrenadeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);
        
        lifetime = 16 * 20;
        
        gravity = 1.0F;
        
        xd = vx;
        yd = vy;
        zd = vz;
        
        quadSize = 0.1F;
        
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
        {
            remove();
            return;
        }
        
        yd -= 0.04D * gravity;
        
        move(xd, yd, zd);
        
        xd *= 0.99D;
        yd *= 0.99D;
        zd *= 0.99D;
        
        if (isInLiquid())
            yd = 1.0D;
        
        dischargeTime--;

        if (dischargeTime < 0)
        {
            level.addParticle(FlansMod.smokeBurstParticle.get(), x, y, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(FlansMod.bigSmokeParticle.get(), x, y, z, 0.0D, 0.0D, 0.0D);
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

            level.addParticle(ParticleTypes.EXPLOSION, px, py, pz, 0.0D, 0.0D, 0.0D);
        }

        if (onGround)
            remove();

        updateVisuals();
    }

    @Override
    protected void updateVisuals()
    {
        quadSize = scaleMultiplier * 0.1F;
        alpha = 1.0F;

        setSpriteFromAge(sprites);
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
            return new SmokeBurstParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
