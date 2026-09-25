package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.AABB;

/** Fiery counterpart of the vanilla explosion particle: a short, fullbright, stationary 16-frame animation. */
public class FireExplosionParticle extends ParticleBase
{
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Quad size before any requested scale, matching the vanilla explosion puff it stands in for. */
    public static final float BASE_SIZE = 2.0F;

    protected FireExplosionParticle(ClientLevel level, double x, double y, double z, double size, SpriteSet sprites)
    {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprites);
        // Same timing and sizing as vanilla HugeExplosionParticle, stretched slightly for 16 frames
        lifetime = 8 + random.nextInt(4);
        float shade = random.nextFloat() * 0.2F + 0.8F;
        rCol = gCol = bCol = shade;
        quadSize = BASE_SIZE * (1.0F - (float) size * 0.5F);
        gravity = 0.0F;
        hasPhysics = false;
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
        else
            updateVisuals();
    }

    @Override
    protected void updateVisuals()
    {
        setSpriteFromAge(sprites);
    }

    @Override
    public Particle scale(float factor)
    {
        // ParticleBase.scale only resizes the collision box, like most legacy particles. This one
        // is sized to its explosion, so a requested scale has to reach the quad itself.
        return applyScale(factor);
    }

    /**
     * NeoForge culls against this box, which by default is the small collision box grown by one
     * block, so a fireball scaled to a large explosion would vanish whenever its centre left the
     * screen. The box is grown to the drawn quad instead.
     */
    @Override
    public AABB getRenderBoundingBox(float partialTicks)
    {
        return getBoundingBox().inflate(Math.max(1.0F, quadSize));
    }

    @Override
    public int getLightColor(float partialTick)
    {
        return FULL_BRIGHT;
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
            // Like vanilla, the x velocity argument carries the size modifier
            return new FireExplosionParticle(level, x, y, z, vx, sprites);
        }
    }
}
