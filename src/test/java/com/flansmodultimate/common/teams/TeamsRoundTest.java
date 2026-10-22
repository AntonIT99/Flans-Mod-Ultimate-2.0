package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void aRoundWithoutSidesPutsEveryTeamOnItsOwn()
    {
        TeamsRound round = new TeamsRound("map", "tdm", List.of("red", "blue", "green"), 10, 30);
        assertEquals(List.of(0, 1, 2), round.getSides());
        assertEquals(3, round.getSideCount());
        assertFalse(round.hasAlliances());
        assertEquals(List.of(2, 3, 4), round.getSideIds());
        assertEquals(3, round.getSideId(3));
    }

    @Test
    void alliedTeamsGoByTheRoundTeamIdOfTheFirstOfThem()
    {
        TeamsRound round = new TeamsRound("map", "tdm", List.of("us", "germany", "uk", "italy"), List.of(7, 3, 7, 3), 10, 30);
        assertEquals(List.of(0, 1, 0, 1), round.getSides());
        assertEquals(2, round.getSideCount());
        assertTrue(round.hasAlliances());
        assertEquals(List.of(2, 3), round.getSideIds());
        assertEquals(2, round.getSideId(4));
        assertEquals(3, round.getSideId(5));
        assertEquals("us+uk vs germany+italy", round.describeTeams());
    }

    @Test
    void idsThatAreNotATeamOfTheRoundHaveNoSide()
    {
        TeamsRound round = new TeamsRound("map", "tdm", List.of("red", "blue"), List.of(0, 0), 10, 30);
        assertEquals(0, round.getSideId(0));
        assertEquals(1, round.getSideId(1));
        assertEquals(9, round.getSideId(9));
    }

    @Test
    void sidesThatDoNotLineUpWithTheTeamsAreIgnored()
    {
        TeamsRound round = new TeamsRound("map", "tdm", List.of("red", "blue", "green"), List.of(0, 0), 10, 30);
        assertEquals(List.of(0, 1, 2), round.getSides());
    }

    @Test
    void sidesAreSavedWithTheRound()
    {
        TeamsRound round = new TeamsRound("map", "con", List.of("us", "uk", "germany"), List.of(0, 0, 1), 10, 300);
        assertEquals(List.of(0, 0, 1), TeamsRound.load(round.save()).getSides());
    }

    @Test
    void aRoundSavedBeforeAlliancesLoadsWithEveryTeamOnItsOwnSide()
    {
        var tag = new TeamsRound("map", "tdm", List.of("red", "blue"), List.of(0, 0), 10, 30).save();
        tag.remove("sides");
        assertEquals(List.of(0, 1), TeamsRound.load(tag).getSides());
    }

    @Test
    void aLineupIsReadFromSidesJoinedByPlusAndSeparatedByCommas()
    {
        TeamsRound.Lineup written = TeamsRound.parseLineup(List.of("us+uk", "germany+italy"));
        assertEquals(List.of("us", "uk", "germany", "italy"), written.teamIds());
        assertEquals(List.of(0, 0, 1, 1), written.sides());

        TeamsRound.Lineup legacy = TeamsRound.parseLineup(List.of("red,blue"));
        assertEquals(List.of("red", "blue"), legacy.teamIds());
        assertEquals(List.of(0, 1), legacy.sides());

        TeamsRound.Lineup untidy = TeamsRound.parseLineup(List.of("red+,", "+blue"));
        assertEquals(List.of("red", "blue"), untidy.teamIds());
        assertEquals(List.of(0, 1), untidy.sides());
    }

    @Test
    void addonDataIsSavedWithTheRoundByKey()
    {
        TeamsRound round = new TeamsRound("map", "conquest", List.of("red", "blue"), 10, 30);
        ResourceLocation npcs = ResourceLocation.fromNamespaceAndPath("wolffsmodnpcs", "bots");
        ResourceLocation other = ResourceLocation.fromNamespaceAndPath("other", "bots");
        CompoundTag data = new CompoundTag();
        data.putInt("count", 24);
        round.setExtensionData(npcs, data);
        data.putInt("count", 99);

        TeamsRound loaded = TeamsRound.load(round.save());
        assertEquals(24, loaded.getExtensionData(npcs).getInt("count"));
        assertTrue(loaded.getExtensionData(other).isEmpty());

        loaded.getExtensionData(npcs).putInt("count", 1);
        assertEquals(24, loaded.getExtensionData(npcs).getInt("count"), "readers get a copy");

        loaded.setExtensionData(npcs, new CompoundTag());
        assertTrue(TeamsRound.load(loaded.save()).getExtensionData(npcs).isEmpty());
    }

    @Test
    void sideIndicesFollowTheOrderSidesWereListedIn()
    {
        TeamsRound round = new TeamsRound("map", "tdm", List.of("us", "germany", "uk", "italy"), List.of(7, 3, 7, 3), 10, 30);
        int firstSide = round.getSideId(TeamsRound.FIRST_TEAM_ID);
        int secondSide = round.getSideId(TeamsRound.FIRST_TEAM_ID + 1);
        assertEquals(0, round.getSideIndex(firstSide));
        assertEquals(1, round.getSideIndex(secondSide));
        assertEquals(-1, round.getSideIndex(0));
        assertEquals(-1, round.getSideIndex(1));
    }

    @Test
    void teamLimitsSayWhatAGameTypeTakes()
    {
        GameType.TeamLimits limits = GameType.TeamLimits.upToSides(2);
        assertNull(limits.describeRefusal(4, 2));
        assertNotNull(limits.describeRefusal(4, 3));
        assertNotNull(limits.describeRefusal(1, 1));
        assertNotNull(limits.describeRefusal(TeamsRound.MAX_TEAMS + 1, 2));
        assertEquals("2 to 8 teams on 2 sides", limits.describe());
        assertEquals("2 teams", GameType.TeamLimits.exactly(2).describe());
    }

    @Test
    void theAttackingSideIsSavedAndOnlyItMayCapture()
    {
        TeamsRound round = new TeamsRound("map", "con", List.of("red", "blue"), 10, 30);
        assertTrue(round.getAttackingSide().isEmpty());
        assertTrue(round.mayCapture(2) && round.mayCapture(3));
        round.setAttackingSide(1);
        TeamsRound loaded = TeamsRound.load(round.save());
        assertEquals(1, loaded.getAttackingSide().getAsInt());
        assertEquals(3, loaded.getAttackingSideId());
        assertTrue(loaded.mayCapture(3));
        assertFalse(loaded.mayCapture(2));
    }

    @Test
    void anAttackingSideTheRoundDoesNotHaveIsDropped()
    {
        TeamsRound round = new TeamsRound("map", "con", List.of("red", "blue"), 10, 30);
        round.setAttackingSide(5);
        assertTrue(round.getAttackingSide().isEmpty());
        round.setAttackingSide(0);
        round.setAttackingSide(-1);
        assertTrue(round.getAttackingSide().isEmpty());
    }
}
