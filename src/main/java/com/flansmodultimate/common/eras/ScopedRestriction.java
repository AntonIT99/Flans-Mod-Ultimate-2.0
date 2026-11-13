package com.flansmodultimate.common.eras;

/** A restriction together with the scope that applies it, so a refusal can say where it came from. */
public record ScopedRestriction(EraScope scope, EraRestriction restriction)
{
    @Override
    public String toString()
    {
        return scope + ": " + restriction;
    }
}
