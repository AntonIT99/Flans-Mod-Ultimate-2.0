package com.flansmodultimate.platform.client;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/** Version boundary for particle options whose shape changed. Client-only. */
public final class ParticlePlatform
{
    private ParticlePlatform() {}

    /**
     * The legacy {@code mobSpell}/{@code mobSpellAmbient} particle, coloured by the spawn velocity as in 1.20.1,
     * whose providers read the colour from the velocity. The ambient variant is drawn at 15% opacity.
     * Each channel is converted like the 1.20.1 float vertex colour, {@code (int)(v * 255)} truncated to a byte.
     */
    public static ParticleOptions entityEffect(boolean ambient, double vx, double vy, double vz)
    {
        int argb = channel(ambient ? 0.15F : 1F) << 24 | channel((float)vx) << 16 | channel((float)vy) << 8 | channel((float)vz);
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, argb);
    }

    private static int channel(float value)
    {
        return (int)(value * 255F) & 0xFF;
    }
}
