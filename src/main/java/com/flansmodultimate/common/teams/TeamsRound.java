package com.flansmodultimate.common.teams;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This represents a round in the teams mod
 * It designates the map, gametype and teams to be played that round
 * A list of valid rounds is kept by the TeamsManager and then either
 * players vote on which rounds to play or there is a rotation
 */
@EqualsAndHashCode
public class TeamsRound implements Comparable<TeamsRound>
{
    @Getter
    private GameType gametype;
    private TeamsMap map;
    /** The teams available. This does not include spectators */
    private final List<Team> teams = new ArrayList<>();
    /** The round length in minutes */
    private int timeLimit;
    /** The round score limit */
    private int scoreLimit;
    /** 0 is almost never picked, 1 is always picked. Used to pick vote options */
    private final float popularity;
    /** Number of rounds since it was offered as an option in the vote. Used to pick vote options */
    private int roundsSincePlayed;

    public TeamsRound(TeamsMap map, GameType gametype, List<Team> teams, int timeLimit, int scoreLimit)
    {
        this.map = map;
        this.gametype = gametype;
        this.teams.addAll(teams);
        this.timeLimit = timeLimit;
        this.scoreLimit = scoreLimit;
        popularity = 0.5F;
    }

    public float getWeight()
    {
        return popularity * 4F + roundsSincePlayed;
    }

    @Override
    public int compareTo(@NotNull TeamsRound o)
    {
        return Float.compare(o.getWeight(), getWeight());
    }
}
