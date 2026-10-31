package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/** One growing smoke puff at its origin, with no ballistic motion or secondary emitters. */
public class SmokeGrenadeParticle extends ParticleBase
{
    protected SmokeGrenadeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites)
    {
        super(level, x, y, z, 0D, 0D, 0D, sprites);
        lifetime = 40;
        hasPhysics = false;
        gravity = 0F;
        xd = 0D;
        yd = 0D;
        zd = 0D;
        rCol = gCol = bCol = 0.9F + random.nextFloat() * 0.1F;
        updateVisuals();
    }

    @Override
    protected void updateVisuals()
    {
        quadSize = scaleMultiplier * (0.75F + 1.75F * Math.min(1F, age / 20F));
        alpha = 0.75F * Math.min(1F, age / 6F) * Math.max(0F, Math.min(1F, (lifetime - age) / 12F));
        setSpriteFromAge(sprites);
    }

    @Override
    @NotNull
    public Particle scale(float factor)
    {
        return applyScale(factor);
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
        {
            // Explosion packets carry random velocities; a smoke cloud must stay at the detonation.
            return new SmokeGrenadeParticle(level, x, y, z, sprites);
        }
    }
}
