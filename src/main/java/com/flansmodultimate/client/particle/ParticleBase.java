package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public abstract class ParticleBase extends TextureSheetParticle
{
    protected final SpriteSet sprites;
    protected float scaleMultiplier = 1F;

    protected ParticleBase(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites)
    {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
    }

    @Override
    public int getLightColor(float partialTick)
    {
        // fullbright by default, override if needed
        return 0xF000F0;
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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

        x += xd;
        y += yd;
        z += zd;

        if (onGround)
            remove();

        // hook for subclasses to do per-tick visuals (scale, alpha, sprite)
        updateVisuals();
    }

    @Override
    public Particle scale(float factor)
    {
        scaleMultiplier = factor;
        return super.scale(factor);
    }

    protected abstract void updateVisuals();
}
