package com.flansmodultimate.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.particle.Particle;

/** Read-only use: the interpolated position of a particle drawn into the thermal heat mask. */
@Mixin(Particle.class)
public interface ParticleAccessor
{
    @Accessor("x")
    double flansmodultimateX();

    @Accessor("y")
    double flansmodultimateY();

    @Accessor("z")
    double flansmodultimateZ();

    @Accessor("xo")
    double flansmodultimateXo();

    @Accessor("yo")
    double flansmodultimateYo();

    @Accessor("zo")
    double flansmodultimateZo();
}
