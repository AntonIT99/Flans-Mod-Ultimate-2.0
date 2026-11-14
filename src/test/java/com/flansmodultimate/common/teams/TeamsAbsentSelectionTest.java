package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamsAbsentSelectionTest
{
    @Test
    void aNewRoundForgetsTheTeamsOfThoseWhoAreAway()
    {
        PlayerStats present = new PlayerStats(UUID.randomUUID(), "Present");
        PlayerStats absent = new PlayerStats(UUID.randomUUID(), "Absent");
        present.setSelection("allies", "rifleman");
        absent.setSelection("axis", "gunner");

        TeamsManager.forgetAbsentSelections(List.of(present, absent), Set.of(present.getPlayerId()));

        assertEquals("allies", present.getSelectedTeam());
        assertEquals("rifleman", present.getSelectedClass());
        // Back on spectators with no class, so logging in opens the team menu instead of resuming a round that is over.
        assertEquals("spectators", absent.getSelectedTeam());
        assertEquals("", absent.getSelectedClass());
    }
}
