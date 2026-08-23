package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

public abstract class ParticleBase extends SingleQuadParticle
{
    protected final SpriteSet sprites;
    protected float scaleMultiplier = 1F;

    protected ParticleBase(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz, sprites.first());
        this.sprites = sprites;
    }

    @Override
    @NotNull
    protected Layer getLayer()
    {
        return LegacyParticleRenderTypes.TRANSLUCENT;
    }

    @Override
    public void tick()
    {
        xo = x;
        yo = y;
        zo = z;

        if (age++ >= lifetime)
            remove();

        move(xd, yd, zd);

        if (onGround)
            remove();

        // hook for subclasses to do per-tick visuals (scale, alpha, sprite)
        updateVisuals();
    }

    @Override
    public Particle scale(float factor)
    {
        // Most legacy layer-3 renderers used a hard-coded quad size and
        // ignored EntityFX.particleScale. multipleParticleScaleBy still
        // changed their collision box, however.
        setSize(0.2F * factor, 0.2F * factor);
        return this;
    }

    protected Particle applyScale(float factor)
    {
        scaleMultiplier *= factor;
        return super.scale(factor);
    }

    protected abstract void updateVisuals();
}
