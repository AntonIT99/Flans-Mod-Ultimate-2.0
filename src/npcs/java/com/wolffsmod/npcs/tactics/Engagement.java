package com.wolffsmod.npcs.tactics;

/**
 * The distances an NPC fights at.
 *
 * @param preferred
 *            blocks from its target the NPC's tactic keeps it at
 * @param firing
 *            blocks within which the NPC fires
 * @param minimum
 *            blocks from its target the NPC never comes within, because its own rounds would catch it there; 0 for one
 *            that fires nothing explosive
 */
public record Engagement(double preferred, double firing, double minimum)
{
    /** Nearest an automatic tactic distance comes, so that a short firing range does not bring the NPC into melee. */
    static final double MIN_AUTOMATIC = 4D;
    /** Share of the preferred distance closer than which a stand-off NPC backs away. */
    static final double NEAR_SHARE = 0.75D;
    /** Blocks beyond the preferred distance a stand-off NPC lets the target go before it follows. */
    static final double FAR_MARGIN = 4D;

    public static Engagement of(double firingRange, int tacticDistance, WeaponRole role)
    {
        return of(firingRange, tacticDistance, role, 0D);
    }

    /**
     * @param tacticDistance
     *            the tactic distance setting; 0 takes the share of the firing range that suits the weapon
     * @param standoff
     *            blocks from its target within which the NPC's own rounds would catch it
     */
    public static Engagement of(double firingRange, int tacticDistance, WeaponRole role, double standoff)
    {
        double firing = Math.max(1D, firingRange);
        double preferred = tacticDistance > 0 ? tacticDistance : Math.max(MIN_AUTOMATIC, firing * role.share());
        // Standing further off than the gun reaches would leave it silent, and nearer than its own blast reaches would wreck it.
        double minimum = Math.min(Math.max(0D, standoff), firing);
        return new Engagement(Math.max(minimum, Math.min(preferred, firing)), firing, minimum);
    }

    /** @return blocks closer than which an NPC that keeps its distance backs away */
    public double near()
    {
        return Math.max(minimum, preferred * NEAR_SHARE);
    }

    /** @return blocks beyond which an NPC that keeps its distance follows its target */
    public double far()
    {
        return Math.max(near(), Math.min(firing, preferred + FAR_MARGIN));
    }
}
