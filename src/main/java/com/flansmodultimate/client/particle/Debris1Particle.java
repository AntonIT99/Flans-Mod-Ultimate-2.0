package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Reimplementation of Flan's Mod 1.7.10's EntityDebris1.
 */
public final class Debris1Particle extends TextureSheetParticle
{
    private Debris1Particle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz);

        // Both the 1.7.10 EntityFX constructor and the modern Particle
        // constructor choose the same randomized base lifetime. The legacy
        // debris particle multiplies that value by five.
        lifetime *= 5;

        gravity = 1.0F;

        xd = vx;
        yd = vy;
        zd = vz;

        // EntityDebris1 never changed EntityFX's default texture index, so it
        // always rendered legacy particles.png tile 0 (modern generic_0).
        pickSprite(sprites);
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

        for (int i = 0; i < NUM; i++)
        {
            double px = xo + dx * i;
            double py = yo + dy * i;
            double pz = zo + dz * i;

            if (age < 10)
            {
                level.addParticle(ParticleTypes.FLAME, px, py, pz, 0.0D, 0.0D, 0.0D);
            }
            
            double pySmoke = yo + dy * i * 2.0D;
            level.addParticle(ParticleTypes.LARGE_SMOKE, px, pySmoke, pz, 0.0D, 0.0D, 0.0D);
        }

        if (onGround)
            remove();
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
        {
            return new Debris1Particle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
