package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.names.SoldierNameAssociations;
import com.wolffsmod.npcs.names.SoldierNames;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotNamesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void classAssociationBeatsTeamAndForceListsAndUsesClassGender()
    {
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("team", "wolffsmodnpcs:clone_ct"), Map.of("Commander", "wolffsmodnpcs:clone_cody"), Map.of("team", "mixed"),
            Map.of("Commander", "female"));
        EntityNPCInterface npc = npc();
        try (var config = mockStatic(SoldierNameAssociations.class))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            assertEquals("Commander Cody", BotFactory.chooseName(npc, order("commander", "german"), "commander", "", new Random(1)));
            verify(npc.display).setMarkovGeneratorId(55);
            verify(npc.display).setMarkovGender(2);
        }
    }

    @Test
    void teamAssociationDefaultsToMaleAndSupportsBuiltInRomanNames()
    {
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("TEAM", SoldierNames.ROMAN_ID), Map.of());
        EntityNPCInterface npc = npc();
        try (var config = mockStatic(SoldierNameAssociations.class); var generator = mockStatic(nikedemos.markovnames.generators.MarkovGenerator.class))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            generator.when(() -> nikedemos.markovnames.generators.MarkovGenerator.fetch(0, 1)).thenReturn("Marcus");
            assertEquals("Marcus", BotFactory.chooseName(npc, order("legionary", "german"), "legionary", "", new Random(1)));
            verify(npc.display).setMarkovGeneratorId(0);
            verify(npc.display).setMarkovGender(1);
        }
    }

    @Test
    void respawningBotKeepsItsNameButRestoresAssociatedGeneratorAndGender()
    {
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("team", "wolffsmodnpcs:french"), Map.of(), Map.of("team", "mixed"), Map.of());
        EntityNPCInterface npc = npc();
        try (var config = mockStatic(SoldierNameAssociations.class))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            assertEquals("Louis Martin", BotFactory.chooseName(npc, order("infantry", "german"), "infantry", "Louis Martin", new Random(1)));
            verify(npc.display).setMarkovGeneratorId(70);
            verify(npc.display).setMarkovGender(0);
        }
    }

    @Test
    void vehicleCrewUsesItsOwnClassAndDoesNotInheritTheVehicleSlotName()
    {
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("team", "wolffsmodnpcs:clone_ct"), Map.of("captain", "wolffsmodnpcs:clone_rex"));
        BotFactory.Order order = order("tank", "german");
        order.slot().setName("Vehicle crew");
        EntityNPCInterface npc = npc();
        try (var config = mockStatic(SoldierNameAssociations.class))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            assertEquals("Captain Rex", BotFactory.chooseName(npc, order, "captain", "", new Random(1)));
            verify(npc.display).setMarkovGeneratorId(54);
            assertEquals("Vehicle crew", order.slot().getName());
        }
    }

    @Test
    void vehicleCrewLabelUsesTeamAssociationWithoutTreatingVehicleAsAClass()
    {
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("team", "wolffsmodnpcs:clone_cody"), Map.of("tank", "wolffsmodnpcs:clone_rex"));
        EntityNPCInterface npc = npc();
        try (var config = mockStatic(SoldierNameAssociations.class))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            assertEquals("Commander Cody", BotFactory.chooseName(npc, order("tank", "german"), "", "", new Random(1)));
            verify(npc.display).setMarkovGeneratorId(55);
        }
    }

    @Test
    void disabledClassAssociationFallsBackToExplicitBotList()
    {
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("team", "wolffsmodnpcs:clone_cody"), Map.of("infantry", ""));
        EntityNPCInterface npc = npc();
        try (var config = mockStatic(SoldierNameAssociations.class))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            assertEquals("Captain Rex", BotFactory.chooseName(npc, order("infantry", "clone_rex"), "infantry", "", new Random(1)));
            verifyNoInteractions(npc.display);
        }
    }

    @Test
    void unmappedBotsKeepExistingFallbacksAndSavedNames()
    {
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of(), Map.of());
        EntityNPCInterface npc = npc();
        try (var config = mockStatic(SoldierNameAssociations.class))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            assertEquals("Captain Rex", BotFactory.chooseName(npc, order("infantry", "clone_rex"), "infantry", "", new Random(1)));
            assertEquals("", BotFactory.chooseName(npc, order("infantry", ""), "infantry", "", new Random(1)));
            assertEquals("Existing clone name", BotFactory.chooseName(npc, order("infantry", ""), "infantry", "Existing clone name", new Random(1)));
            verifyNoInteractions(npc.display);
        }
    }

    private static EntityNPCInterface npc()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.display = mock(DataDisplay.class);
        return npc;
    }

    private static BotFactory.Order order(String role, String nameList)
    {
        ITeam team = mock(ITeam.class);
        when(team.getShortName()).thenReturn("team");
        BotSlot slot = new BotSlot(1, "team", BotSlot.Source.INFANTRY, -1, true);
        slot.setRole(role);
        return new BotFactory.Order(slot, team, 1, new BotSettings(), new BotSettings.TeamForce(), UUID.randomUUID(), Vec3.ZERO, nameList, true);
    }
}
