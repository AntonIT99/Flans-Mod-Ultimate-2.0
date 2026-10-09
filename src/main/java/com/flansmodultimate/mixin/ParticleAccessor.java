package com.flansmodultimate.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only use: the interpolated position of a particle drawn into the thermal heat mask. */
@Mixin(Particle.class)
public interface ParticleAccessor
{
    @Accessor("x")
    double flansmodultimate$x();

    @Accessor("y")
    double flansmodultimate$y();

    @Accessor("z")
    double flansmodultimate$z();

    @Accessor("xo")
    double flansmodultimate$xo();

    @Accessor("yo")
    double flansmodultimate$yo();

    @Accessor("zo")
    double flansmodultimate$zo();
}
