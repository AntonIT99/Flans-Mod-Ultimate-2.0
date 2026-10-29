package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.item.ItemStackData;
import com.wolffsmod.npcs.names.FactionDisplayName;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamFactionsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void reusesRenamedEditedFactionAfterReloadAndRecreatesDeletedFaction()
    {
        FactionController previous = FactionController.instance;
        FactionController controller = mock(FactionController.class);
        FactionController.instance = controller;
        try
        {
            ITeam team = mock(ITeam.class);
            when(team.getShortName()).thenReturn("testTeam");
            when(team.getName()).thenReturn("Test Team");
            when(team.getTeamColour()).thenReturn(0x123456);
            Faction faction = mock(Faction.class, withSettings().extraInterfaces(FactionDisplayName.class));
            faction.id = 8;
            faction.hideFaction = true;
            when(controller.create("Test Team", 0x123456)).thenReturn(faction);
            when(controller.getFaction(8)).thenReturn(faction);
            TeamFactions data = new TeamFactions();
            assertSame(faction, data.getOrCreate(team));
            assertFalse(faction.hideFaction);
            verify(controller).saveFaction(faction);
            faction.name = "Renamed by builder";
            faction.color = 0xABCDEF;
            TeamFactions reloaded = TeamFactions.load(data.save(new CompoundTag(), ItemStackData.builtInRegistries()), ItemStackData.builtInRegistries());
            EntityNPCInterface npc = mock(EntityNPCInterface.class);
            reloaded.apply(npc, team);
            assertSame(faction, npc.faction);
            verify(npc).setFaction(8);
            assertTrue(npc.updateClient);
            assertTrue(npc.updateAI);
            assertEquals("Renamed by builder", faction.name);
            assertEquals(0xABCDEF, faction.color);
            verify(controller, times(1)).create(anyString(), anyInt());
            verify(controller, times(1)).saveFaction(any());

            Faction replacement = mock(Faction.class, withSettings().extraInterfaces(FactionDisplayName.class));
            replacement.id = 9;
            when(controller.getFaction(8)).thenReturn(null);
            when(controller.create("Test Team", 0x123456)).thenReturn(replacement);
            assertSame(replacement, reloaded.getOrCreate(team));
            when(controller.getFaction(9)).thenReturn(replacement);
            assertSame(replacement, reloaded.getOrCreate(team));
            verify(controller, times(2)).create(anyString(), anyInt());
        }
        finally
        {
            FactionController.instance = previous;
        }
    }

    @Test
    void adoptsExistingEquivalentFactionWithoutOverwritingItsSettings()
    {
        FactionController previous = FactionController.instance;
        FactionController controller = mock(FactionController.class);
        FactionController.instance = controller;
        try
        {
            ITeam team = mock(ITeam.class);
            when(team.getShortName()).thenReturn("existing");
            when(team.getName()).thenReturn("Existing Team");
            Faction faction = mock(Faction.class, withSettings().extraInterfaces(FactionDisplayName.class));
            faction.id = 0;
            faction.color = 0x654321;
            when(controller.getFactionFromName("Existing Team")).thenReturn(faction);
            when(controller.getFaction(0)).thenReturn(faction);
            TeamFactions data = new TeamFactions();
            assertSame(faction, data.getOrCreate(team));
            TeamFactions reloaded = TeamFactions.load(data.save(new CompoundTag(), ItemStackData.builtInRegistries()), ItemStackData.builtInRegistries());
            assertSame(faction, reloaded.getOrCreate(team));
            assertEquals(0x654321, faction.color);
            verify(controller, never()).create(anyString(), anyInt());
            verify(controller, never()).saveFaction(any());
        }
        finally
        {
            FactionController.instance = previous;
        }
    }
}
