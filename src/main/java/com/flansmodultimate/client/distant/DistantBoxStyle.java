package com.flansmodultimate.client.distant;

/** How a group of distant boxes is lit and blended. */
public enum DistantBoxStyle
{
    /** Opaque and lit by the sky, like a vehicle hull. */
    SOLID,
    /** Lights itself, like a fireball or a muzzle flash. */
    GLOW,
    /** Translucent and lit by the sky, like smoke. */
    SMOKE
}
