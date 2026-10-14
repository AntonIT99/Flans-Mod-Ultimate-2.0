package com.flansmodultimate.common.teams;

/** Built-in game types: those of the legacy Teams module, and the objective modes of the Labjac fork. */
public final class GameTypes
{
    public static final GameType DEATHMATCH = new GameTypeDM();
    public static final GameType TEAM_DEATHMATCH = new GameTypeTDM();
    public static final GameType CAPTURE_THE_FLAG = new GameTypeCTF();
    public static final GameType ZOMBIES = new GameTypeZombies();
    public static final GameType DOMINATION = new GameTypeDomination();
    public static final GameType CONQUEST = new GameTypeConquest();
    public static final GameType ASSAULT = new GameTypeAssault();

    private GameTypes()
    {}

    static void bootstrap()
    {
        // Only forces the class to load so its game types register.
    }
}
