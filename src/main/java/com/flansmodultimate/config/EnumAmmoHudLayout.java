package com.flansmodultimate.config;

import net.minecraft.world.InteractionHand;

/** Selects the current ammo HUD or one of the two layouts from Ultimate 1.7.10. */
public enum EnumAmmoHudLayout
{
    CURRENT(null, null),
    LEGACY_FANCY(new Placement(92, 8, 90, 25), new Placement(75, 8, 107, 25)),
    LEGACY_DEFAULT(new Placement(32, 59, 16, 65), new Placement(16, 59, 32, 65));

    private final Placement mainHand;
    private final Placement offHand;

    EnumAmmoHudLayout(Placement mainHand, Placement offHand)
    {
        this.mainHand = mainHand;
        this.offHand = offHand;
    }

    public boolean isLegacy()
    {
        return this != CURRENT;
    }

    public Placement placement(InteractionHand hand)
    {
        if (!isLegacy())
            throw new IllegalStateException("The current ammo HUD does not use legacy placement offsets");
        return hand == InteractionHand.MAIN_HAND ? mainHand : offHand;
    }

    public record Placement(int textX, int textY, int iconX, int iconY) {}
}
