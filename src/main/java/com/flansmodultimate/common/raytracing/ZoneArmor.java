package com.flansmodultimate.common.raytracing;

/**
 * Per-zone body armour against small-arms rounds, from the Labjac Edition. Every worn piece rates each zone it covers. A round
 * whose body-armour penetration falls short of 85% of a rating does not penetrate and keeps its {@code NonPenPenalty} share of
 * damage; one between 85% and the full rating barely penetrates and keeps its {@code BarelyPenPenalty} share. Pieces covering
 * the same zone each apply their own penalty.
 */
public final class ZoneArmor
{
    /** Share of a rating below which a round does not penetrate at all */
    public static final float NON_PENETRATION_SHARE = 0.85F;

    public enum Result
    {
        PENETRATED, BARELY_PENETRATED, NOT_PENETRATED
    }

    private ZoneArmor()
    {}

    /**
     * @param penetration the round's body-armour penetration
     * @param rating      the piece's rating for the struck zone; zero or less means the piece does not cover it
     */
    public static Result resolve(float penetration, float rating)
    {
        if (!(rating > 0F))
            return Result.PENETRATED;
        if (penetration < rating * NON_PENETRATION_SHARE)
            return Result.NOT_PENETRATED;
        if (penetration < rating)
            return Result.BARELY_PENETRATED;
        return Result.PENETRATED;
    }

    /** @return the share of damage that passes through the piece */
    public static float damageFactor(Result result, float nonPenPenalty, float barelyPenPenalty)
    {
        return switch (result)
        {
            case NOT_PENETRATED -> nonPenPenalty;
            case BARELY_PENETRATED -> barelyPenPenalty;
            case PENETRATED -> 1F;
        };
    }
}
