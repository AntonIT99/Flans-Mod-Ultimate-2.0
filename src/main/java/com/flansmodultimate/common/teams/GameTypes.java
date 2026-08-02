package com.flansmodultimate.common.teams;

/** Built-in game types matching the legacy Teams module. */
public final class GameTypes
{
    public static final GameType DEATHMATCH = new GameTypeDM();
    public static final GameType TEAM_DEATHMATCH = new GameTypeTDM();
    public static final GameType CAPTURE_THE_FLAG = new GameTypeCTF();
    public static final GameType ZOMBIES = new GameTypeZombies();

    private GameTypes() {}
    static void bootstrap() {}
}
