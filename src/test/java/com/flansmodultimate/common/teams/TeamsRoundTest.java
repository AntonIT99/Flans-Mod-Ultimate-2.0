package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamsRoundTest
{
    @Test
    void scoreLimitChangedDuringARoundIsSavedWithIt()
    {
        TeamsRound round = new TeamsRound("map", "tdm", List.of("red", "blue"), 10, 30);
        round.setScoreLimit(50);
        TeamsRound loaded = TeamsRound.load(round.save());
        assertEquals(50, loaded.getScoreLimit());
        assertEquals(round.getId(), loaded.getId());
    }

    @Test
    void scoreLimitNeverFallsBelowOne()
    {
        TeamsRound round = new TeamsRound("map", "dm", List.of("red"), 10, 20);
        round.setScoreLimit(0);
        assertEquals(1, round.getScoreLimit());
    }
}
