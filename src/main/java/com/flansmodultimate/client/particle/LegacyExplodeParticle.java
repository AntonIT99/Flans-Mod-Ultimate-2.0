package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Reimplementation of Minecraft 1.7.10's EntityExplodeFX.
 */
public final class LegacyExplodeParticle extends TextureSheetParticle
{
    private final SpriteSet sprites;

    private LegacyExplodeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;

        xd = vx + (random.nextDouble() * 2.0D - 1.0D) * 0.05D;
        yd = vy + (random.nextDouble() * 2.0D - 1.0D) * 0.05D;
        zd = vz + (random.nextDouble() * 2.0D - 1.0D) * 0.05D;

        float shade = random.nextFloat() * 0.3F + 0.7F;
        rCol = shade;
        gCol = shade;
        bCol = shade;

        // EntityFX rendered particleScale at one tenth of its value. Store the
        // equivalent rendered size in modern SingleQuadParticle units.
        quadSize = random.nextFloat() * random.nextFloat() * 0.6F + 0.1F;
        lifetime = (int)(16.0D / (random.nextFloat() * 0.8D + 0.2D)) + 2;

        setLegacySprite(7);
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

        setLegacySprite(Mth.clamp(7 - age * 8 / lifetime, 0, 7));

        yd += 0.004D;
        move(xd, yd, zd);

        xd *= 0.8999999761581421D;
        yd *= 0.8999999761581421D;
        zd *= 0.8999999761581421D;

        if (onGround)
        {
            xd *= 0.699999988079071D;
            zd *= 0.699999988079071D;
        }
    }

    private void setLegacySprite(int textureIndex)
    {
        // SpriteSet has no direct indexed getter. With eight sprites, using 7
        // as the maximum age maps 0..7 exactly onto the eight list entries.
        setSprite(sprites.get(textureIndex, 7));
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return LegacyParticleRenderTypes.TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
        {
            return new LegacyExplodeParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
