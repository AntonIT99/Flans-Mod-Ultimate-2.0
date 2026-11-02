package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.ITeam;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** The Teams command centre's pages for factions, skirmishes, wars and bots run these commands, word for word as they write them. */
class BattleCommandLinesTest
{
    private static final CommandDispatcher<CommandSourceStack> DISPATCHER = new CommandDispatcher<>();

    @Test
    void delayOnlyCommandPreservesTheVehicleAllocation() throws CommandSyntaxException
    {
        BotSettings.TeamForce force = new BotSettings.TeamForce();
        BotSettings.VehicleEntry original = new BotSettings.VehicleEntry("44_Tiger2", 2, 5, 0, 3, UUID.randomUUID(), "squad", List.of(1, 2));
        force.getVehicles().add(original);
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(TeamForceCommands.build(Commands.literal("force"), new TeamForceCommands.Target()
        {
            @Override
            public Optional<ITeam> team(CommandContext<CommandSourceStack> context)
            {
                return Optional.empty();
            }

            @Override
            public int edit(CommandContext<CommandSourceStack> context, Consumer<BotSettings.TeamForce> change, Component message)
            {
                change.accept(force);
                return 1;
            }
        }));
        CommandSourceStack operator = mock(CommandSourceStack.class);
        assertEquals(1, dispatcher.execute("force vehiclerespawn 44_Tiger2 120", operator));
        BotSettings.VehicleEntry changed = force.getVehicles().get(0);
        assertEquals(new BotSettings.VehicleEntry(original.type(), original.count(), original.budget(), 120, original.ticketCost(), original.spawner(), original.carry(), original.paintjobs()),
            changed);
        assertEquals(1, dispatcher.execute("force vehiclerespawn 44_Tiger2 inherit", operator));
        assertEquals(original, force.getVehicles().get(0));
    }

    @BeforeAll
    static void registerCommands()
    {
        // The dimension argument of the war commands needs the vanilla registries.
        SharedConstants.tryDetectVersion();
        try
        {
            Bootstrap.bootStrap();
        }
        catch (ExceptionInInitializerError error)
        {
            // Plain JUnit lacks the NetworkEvent transformer of Forge; vanilla registries are initialized first.
            Throwable cause = error;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (!(cause instanceof NoSuchMethodException) || !cause.getMessage().equals("net.minecraftforge.network.NetworkEvent.<init>()"))
                throw error;
        }
        BotCommands.register(DISPATCHER);
        FactionCommands.register(DISPATCHER);
        SkirmishCommands.register(DISPATCHER);
        WarCommands.register(DISPATCHER);
    }

    // The source of a parse has no level or server, which parsing never asks for.
    @SuppressWarnings("DataFlowIssue")
    private static void assertRuns(String command)
    {
        // Parsing only asks the source for its permission level, so it needs neither a level nor a server.
        CommandSourceStack operator = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 4, "test", Component.literal("test"), null, null);
        ParseResults<CommandSourceStack> results = DISPATCHER.parse(command, operator);
        assertFalse(results.getReader().canRead(), "Unparsed input in: " + command);
        assertTrue(results.getExceptions().isEmpty(), "Parse errors in: " + command);
        assertNotNull(results.getContext().build(command).getCommand(), "Not executable: " + command);
    }

    @Test
    void factionLines()
    {
        List.of("teams faction create \"Red Army\"", "teams faction create \"Red Army\" FF4040", "teams faction create Wehrmacht from germany", "teams faction outsiders \"Red Army\" points",
            "teams faction join \"Red Army\"", "teams faction leave", "teams faction remove \"Red Army\"", "teams faction hostile \"Red Army\" Wehrmacht", "teams faction peace \"Red Army\" Wehrmacht",
            "teams faction branch \"Red Army\" add ussr", "teams faction branch \"Red Army\" remove ussr", "teams faction branch \"Red Army\" ussr weight 2",
            "teams faction branch \"Red Army\" ussr count 12", "teams faction branch \"Red Army\" ussr roles infantry armour", "teams faction branch \"Red Army\" ussr roles auto")
            .forEach(BattleCommandLinesTest::assertRuns);
    }

    @Test
    void skirmishLines()
    {
        List.of("teams skirmish create \"Battle of Kursk\"", "teams skirmish quick \"Red Army\" Wehrmacht 16 96", "teams skirmish start \"Battle of Kursk\"",
            "teams skirmish pause \"Battle of Kursk\"", "teams skirmish resume \"Battle of Kursk\"", "teams skirmish stop \"Battle of Kursk\"", "teams skirmish clear",
            "teams skirmish side \"Battle of Kursk\" add \"Red Army\" 16", "teams skirmish side \"Battle of Kursk\" remove \"Red Army\"",
            "teams skirmish force \"Battle of Kursk\" \"Red Army\" infantry 20", "teams skirmish force \"Battle of Kursk\" \"Red Army\" listed", "teams skirmish set \"Battle of Kursk\" intent push",
            "teams skirmish set \"Battle of Kursk\" difficulty veteran", "teams skirmish set \"Battle of Kursk\" end time 20", "teams skirmish set \"Battle of Kursk\" end wipeout",
            "teams skirmish set \"Battle of Kursk\" respawn 30", "teams skirmish set \"Battle of Kursk\" respawn off", "teams skirmish set \"Battle of Kursk\" tickets 100",
            "teams skirmish set \"Battle of Kursk\" protection off", "teams skirmish preset save \"Battle of Kursk\" kursk", "teams skirmish preset apply kursk \"Battle of Prokhorovka\" here",
            "teams skirmish preset apply kursk \"Battle of Prokhorovka\"", "teams skirmish preset remove kursk").forEach(BattleCommandLinesTest::assertRuns);
    }

    @Test
    void warLines()
    {
        List.of("teams war create \"Eastern Front\"", "teams war delete \"Eastern Front\"", "teams war start \"Eastern Front\"", "teams war stop \"Eastern Front\"",
            "teams war faction \"Eastern Front\" add \"Red Army\"", "teams war faction \"Eastern Front\" remove \"Red Army\"", "teams war claim \"Eastern Front\" \"Red Army\" 2",
            "teams war unclaim \"Eastern Front\" 2", "teams war set \"Eastern Front\" infantry 12", "teams war set \"Eastern Front\" respawn 30", "teams war set \"Eastern Front\" tickets 0",
            "teams war set \"Eastern Front\" range 128", "teams war set \"Eastern Front\" engagements 3", "teams war set \"Eastern Front\" flip 60", "teams war set \"Eastern Front\" difficulty elite",
            "teams war scope \"Eastern Front\" all", "teams war scope \"Eastern Front\" here", "teams war scope \"Eastern Front\" add minecraft:the_nether",
            "teams war scope \"Eastern Front\" remove minecraft:the_nether").forEach(BattleCommandLinesTest::assertRuns);
    }

    @Test
    void botLines()
    {
        List.of("teams ai editor 2", "teams ai round 2 enable", "teams ai round 2 disable", "teams ai round current vehiclerespawn 30", "teams ai round 2 vehiclerespawn 0",
            "teams ai round 2 team germany vehiclerespawn TigerII 120", "teams ai round 2 team germany vehiclerespawn TigerII inherit", "teams ai preset apply infantry 2",
            "teams ai preset apply infantry all", "teams ai marker stalingrad add garrison", "teams ai marker stalingrad remove", "teams ai marker stalingrad show", "teams ai marker stalingrad clear",
            "teams ai clear").forEach(BattleCommandLinesTest::assertRuns);
    }
}
