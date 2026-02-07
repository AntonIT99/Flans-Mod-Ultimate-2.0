package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansMod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;

public class BigSmokeParticle extends ParticleBase
{
    private int disperseTimer;

    protected BigSmokeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);

        lifetime = 300;

        gravity = 1.0F;

        xd = vx;
        yd = vy;
        zd = vz;

        quadSize = 0.0F;
        alpha = 0.0F;

        disperseTimer = 10;
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

        disperseTimer--;

        if (disperseTimer < 0)
        {
            double dx = (x - xo);
            double dy = (y - yo);
            double dz = (z - zo);

            double rx = xo + dx * 1 + 5 * level.random.nextDouble();
            double ry = yo + dy * 1 + 7 * level.random.nextDouble();
            double rz = zo + dz * 1 + 5 * level.random.nextDouble();

            level.addParticle(FlansMod.rocketExhaustParticle.get(), rx, ry, rz, 0.0D, 0.0D, 0.0D);

            disperseTimer = 2;
        }
        else if (isInWater())
        {
            yd *= 0.89D;
            yd += 0.1D;
        }

        updateVisuals();
    }

    @Override
    protected void updateVisuals()
    {
        quadSize = 0.0F;
        alpha = 0.0F;
    }

    private boolean isInWater()
    {
        return level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);
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
