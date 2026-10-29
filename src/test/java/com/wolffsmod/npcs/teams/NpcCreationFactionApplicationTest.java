package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wolffsmod.npcs.NpcFactionCommands;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcCreationFactionApplicationTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void commandsAssignOnlyTheExecutingPlayerAndResetRestoresNormalCreation() throws CommandSyntaxException
    {
        FactionController previous = FactionController.instance;
        FactionController controller = mock(FactionController.class);
        FactionController.instance = controller;
        try (var lookup = mockStatic(NpcCreationFactions.class))
        {
            MinecraftServer server = mock(MinecraftServer.class);
            NpcCreationFactions settings = new NpcCreationFactions();
            lookup.when(() -> NpcCreationFactions.get(server)).thenReturn(settings);
            lookup.when(() -> NpcCreationFactions.apply(any(), any())).thenCallRealMethod();
            lookup.when(() -> NpcCreationFactions.applyCommand(any(), any())).thenCallRealMethod();
            ServerPlayer alice = mock(ServerPlayer.class);
            ServerPlayer bob = mock(ServerPlayer.class);
            UUID aliceId = UUID.randomUUID();
            when(alice.getUUID()).thenReturn(aliceId);
            when(bob.getUUID()).thenReturn(UUID.randomUUID());
            when(alice.getServer()).thenReturn(server);
            when(bob.getServer()).thenReturn(server);
            CommandSourceStack source = mock(CommandSourceStack.class);
            when(source.hasPermission(2)).thenReturn(true);
            when(source.getPlayerOrException()).thenReturn(alice);
            when(source.getEntity()).thenReturn(alice);
            when(source.getServer()).thenReturn(server);
            Faction faction = mock(Faction.class);
            faction.id = 7;
            faction.name = "Test Faction";
            when(controller.getFactionFromName(faction.name)).thenReturn(faction);
            when(controller.getFaction(7)).thenReturn(faction);
            CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
            NpcFactionCommands.register(dispatcher);

            assertEquals(1, dispatcher.execute("flansnpcfaction set \"Test Faction\"", source));
            assertEquals(OptionalInt.of(7), settings.factionOf(aliceId));
            assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("flansnpcfaction set missing", source));
            assertEquals(OptionalInt.of(7), settings.factionOf(aliceId));
            EntityNPCInterface clone = mock(EntityNPCInterface.class);
            EntityNPCInterface passenger = mock(EntityNPCInterface.class);
            when(clone.getSelfAndPassengers()).thenReturn(Stream.of(clone, passenger));
            NpcCreationFactions.applyCommand(source, clone);
            verify(clone).setFaction(7);
            verify(passenger).setFaction(7);
            assertSame(faction, clone.faction);
            assertTrue(clone.updateClient);

            EntityNPCInterface otherPlayersNpc = mock(EntityNPCInterface.class);
            NpcCreationFactions.apply(bob, otherPlayersNpc);
            verifyNoInteractions(otherPlayersNpc);
            assertEquals(1, dispatcher.execute("flansnpcfaction reset", source));
            EntityNPCInterface afterReset = mock(EntityNPCInterface.class);
            NpcCreationFactions.apply(alice, afterReset);
            verifyNoInteractions(afterReset);

            settings.set(aliceId, 99);
            EntityNPCInterface afterDeletion = mock(EntityNPCInterface.class);
            NpcCreationFactions.apply(alice, afterDeletion);
            verifyNoInteractions(afterDeletion);

            CommandSourceStack console = mock(CommandSourceStack.class);
            EntityNPCInterface automaticNpc = mock(EntityNPCInterface.class);
            NpcCreationFactions.applyCommand(console, automaticNpc);
            verifyNoInteractions(automaticNpc);
        }
        finally
        {
            FactionController.instance = previous;
        }
    }
}
