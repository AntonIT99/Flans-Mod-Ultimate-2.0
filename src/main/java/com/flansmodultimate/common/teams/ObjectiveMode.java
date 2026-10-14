package com.flansmodultimate.common.teams;

/** How a game type uses capture points, which decides the HUD and the respawn screen clients show. */
public enum ObjectiveMode
{
    NONE, DOMINATION, CONQUEST, ASSAULT;

    /** Whether dead players choose where to come back on the deployment map. */
    public boolean usesDeployment()
    {
        return this == CONQUEST || this == ASSAULT;
    }

    public static ObjectiveMode byOrdinal(int ordinal)
    {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }
}
