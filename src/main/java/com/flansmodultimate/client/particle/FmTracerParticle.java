package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansMod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class FmTracerParticle extends ParticleBase
{
    protected FmTracerParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites);

        lifetime = 6;

        gravity = 0.0F;

        xd = vx;
        yd = vy;
        zd = vz;

        quadSize = 0.6F;

        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 1.0F;
        alpha = 1.0F;

        setSpriteFromAge(sprites);

        level.addParticle(FlansMod.fmSmokeParticle.get(), x, y, z, 0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void updateVisuals()
    {
        float scale = 0.6F - age * 0.1F;
        quadSize = scaleMultiplier * Math.max(scale, 0.0F);
        
        float gb = Mth.clamp(1.0F - age * 0.2F, 0.0F, 1.0F);
        rCol = 1.0F;
        gCol = gb;
        bCol = gb;

        alpha = Mth.clamp(1.0F - age * 0.1F, 0.0F, 1.0F);

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
