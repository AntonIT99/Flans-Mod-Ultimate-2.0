package com.wolffsmod.npcs.properties;

/** Health conversion for type changes and old saves, without inventing healing or losing damage to temporary clamps. */
public final class NpcTypeHealth
{
    public static final String SAVED_MAXIMUM = "WolffsModTypeMaxHealth";

    private NpcTypeHealth()
    {}

    public static float fraction(float health, float maximum)
    {
        if (health <= 0F)
            return 0F;
        return maximum > 0F ? Math.max(0F, Math.min(1F, health / maximum)) : 1F;
    }

    public static float restore(float savedHealth, float savedMaximum, float currentMaximum)
    {
        return fraction(savedHealth, savedMaximum) * Math.max(0F, currentMaximum);
    }
}
