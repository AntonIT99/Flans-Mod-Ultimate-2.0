package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.InfoType;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Team extends InfoType
{
    public static final Team SPECTATORS = new Team("spectators", "Spectators", 0x404040);

    protected int teamColour = 0xFFFFFF;

    public Team(String shortname, String s1, int teamCol)
    {
        originalShortName = shortname;
        name = s1;
        teamColour = teamCol;
    }
}
