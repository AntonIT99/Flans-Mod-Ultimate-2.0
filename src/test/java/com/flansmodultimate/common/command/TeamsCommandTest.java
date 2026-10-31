package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import org.junit.jupiter.api.Test;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamsCommandTest
{
    private static ParseResults<CommandSourceStack> parse(String command)
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        TeamsCommand.register(dispatcher);
        // Parsing only asks the source for its permission level, so it needs neither a level nor a server.
        CommandSourceStack operator = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 4, "test", Component.literal("test"), null, null);
        return dispatcher.parse(command, operator);
    }

    private static void assertRuns(String command)
    {
        ParseResults<CommandSourceStack> results = parse(command);
        assertFalse(results.getReader().canRead(), "Unparsed input in: " + command);
        assertTrue(results.getExceptions().isEmpty(), "Parse errors in: " + command);
        assertNotNull(results.getContext().build(command).getCommand(), "Not executable: " + command);
    }

    @Test
    void regenerationAndStopAreAvailableAtTheRoot()
    {
        assertRuns("teams regeneration true");
        assertRuns("teams regeneration false");
        assertRuns("teams stop");
        assertRuns("teams admin stop");
    }

    @Test
    void aRoundIsAddedFromSidesSeparatedByVs()
    {
        assertRuns("teams admin round add arena tdm red vs blue 15 50");
        assertRuns("teams admin round add arena con us+uk vs germany+italy 20 300");
        assertRuns("teams addRound arena dom red vs blue vs green vs yellow 15 300");
    }

    @Test
    void theOlderQuotedCommaListIsStillAccepted()
    {
        assertRuns("teams admin round add arena tdm \"red,blue\" 15 50");
    }

    @Test
    void aCommaListHasToBeQuotedBecauseCommandWordsCannotHoldCommas()
    {
        assertTrue(parse("teams admin round add arena tdm red,blue 15 50").getReader().canRead());
    }

    @Test
    void aRoundNeedsItsLengthAndScoreLimit()
    {
        assertNull(parse("teams admin round add arena tdm red vs blue").getContext().build("").getCommand());
    }

    @Test
    void noMoreThanFourSidesAreTaken()
    {
        assertTrue(parse("teams admin round add arena tdm a vs b vs c vs d vs e 15 50").getReader().canRead());
    }

    @Test
    void alliedSpawnSharingIsARule()
    {
        assertRuns("teams alliedSpawns true");
    }

    @Test
    void theCommandCentreOpensFromTheAdminCommandOrItsOwn()
    {
        assertRuns("teams admin");
        assertRuns("teams gui");
    }

    @Test
    void aRoundOfTheRotationIsMovedAndGivenNewLimits()
    {
        assertRuns("teams admin round move 3 0");
        assertRuns("teams admin round limits 2 25 400");
        assertNull(parse("teams admin round limits 2 25").getContext().build("").getCommand());
    }

    /** What the command centre's pages run, word for word as they write it. */
    @Test
    void everyLineTheCommandCentreWritesIsACommand()
    {
        List<String> lines = new ArrayList<>(List.of("teams admin enabled true", "teams admin next", "teams admin stop", "teams admin kit", "teams admin start 2", "teams admin setnext 2",
            "teams admin round attackers 1 none", "teams admin round attackers 1 2", "teams admin round remove 1", "teams admin round add stalingrad con us+uk vs germany 15 300",
            "teams admin map add stalingrad Stalingrad Factory", "teams admin map rename stalingrad Stalingrad", "teams admin map image stalingrad stalingrad_map",
            "teams admin map bounds stalingrad -10 60 -10 120 90 140", "teams admin map clearBounds stalingrad", "teams admin map remove stalingrad", "teams weaponDrops smart", "teams seaLevel 62",
            "teams seaLevel default", "teams admin arena", "teams admin survival", "teams admin setvariable scorelimit 300", "teams admin setvariable friendlyfire true", "teams admin voting false",
            "teams admin roundsGenerator true", "teams admin votingTime 20", "teams admin scoreDisplayTime 10", "teams admin rankUpdateTime 0", "teams admin autobalancetime 120",
            "teams admin motd Welcome to the front", "teams admin xpmultiplier 1.5", "teams admin loadoutpool none", "teams admin bltss 2 10"));
        com.flansmodultimate.common.teams.centre.TeamsSection.SWITCHES.forEach(rule -> lines.add("teams " + rule + " true"));
        com.flansmodultimate.common.teams.centre.TeamsSection.LIFETIMES.forEach(rule -> lines.add("teams " + rule + " 120"));
        lines.forEach(TeamsCommandTest::assertRuns);
    }
}
