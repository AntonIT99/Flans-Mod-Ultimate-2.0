package com.flansmodultimate.platform.client;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/** Version boundary for particle options whose shape changed. Client-only. */
public final class ParticlePlatform
{
    private ParticlePlatform() {}

    /**
     * The legacy {@code mobSpell}/{@code mobSpellAmbient} particle, coloured by the spawn velocity as in 1.20.1,
     * whose providers read the colour from the velocity. The ambient variant is drawn at 15% opacity.
     */
    public static ParticleOptions entityEffect(boolean ambient, double vx, double vy, double vz)
    {
        return ambient ? ParticleTypes.AMBIENT_ENTITY_EFFECT : ParticleTypes.ENTITY_EFFECT;
    }
}
