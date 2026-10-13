package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import org.junit.jupiter.api.Test;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

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
}
