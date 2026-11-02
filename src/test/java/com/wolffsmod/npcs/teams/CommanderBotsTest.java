package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.tactics.NpcTacticGoal;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.LivingEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommanderBotsTest
{
    @Test
    void meleeCommanderHoldsTheRearAndOnlyYieldsToCloseCombat() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        LivingEntity target = mock(LivingEntity.class);
        npc.stats = mock(DataStats.class);
        npc.stats.melee = mock(DataMelee.class);
        when(npc.stats.melee.getRange()).thenReturn(2);
        when(npc.isAlive()).thenReturn(true);
        when(npc.getTarget()).thenReturn(target);
        when(target.isAlive()).thenReturn(true);
        try (var bots = mockStatic(TeamsBots.class))
        {
            bots.when(() -> TeamsBots.isCommander(npc)).thenReturn(true);
            NpcTacticGoal goal = new NpcTacticGoal(npc);
            assertTrue(goal.canUse(), "a melee loadout must not enable pursuit");
            when(npc.isInRange(target, 2D)).thenReturn(true);
            assertFalse(goal.canUse(), "close enemies can still be fought in melee");
            when(npc.isInRange(target, 2D)).thenReturn(false);
            when(target.isAlive()).thenReturn(false);
            assertFalse(goal.canUse());
        }
    }

    @Test
    void eachSideUsesItsOwnClassAndDisablingRetiresQueuedCommanders()
    {
        ITeam red = team("red", "redOfficer");
        ITeam blue = team("blue", "blueOfficer");
        Battleground ground = mock(Battleground.class);
        when(ground.teams()).thenReturn(List.of(red, blue));
        when(ground.team("red")).thenReturn(Optional.of(red));
        when(ground.team("blue")).thenReturn(Optional.of(blue));
        when(ground.sideOf(red)).thenReturn(0);
        when(ground.sideOf(blue)).thenReturn(1);
        BotSettings settings = new BotSettings();
        settings.team("red").setCommanderClass("redOfficer");
        settings.team("blue").setCommanderClass("blueOfficer");
        BotBattle battle = new BotBattle(UUID.randomUUID(), ground, settings, Map.of());
        CommanderBots.keep(battle);
        assertTrue(battle.getSlots().isEmpty());
        settings.setCommanderBot(true);
        CommanderBots.keep(battle);
        CommanderBots.keep(battle);
        assertEquals(2, battle.getSlots().size());
        List<BotSlot> commanders = List.copyOf(battle.getSlots().values());
        assertEquals("redOfficer", commanders.get(0).getRole());
        assertEquals("blueOfficer", commanders.get(1).getRole());
        assertTrue(BotPopulation.canSpawn(battle, commanders.get(0)));
        commanders.get(0).setSpawns(1);
        settings.setTickets(1);
        battle.getTickets().put(0, 0);
        assertFalse(BotPopulation.canSpawn(battle, commanders.get(0)), "normal respawn tickets apply");
        settings.setCommanderBot(false);
        CommanderBots.keep(battle);
        assertTrue(commanders.stream().allMatch(BotSlot::isRetired));
        assertFalse(BotPopulation.canSpawn(battle, commanders.get(1)));
        assertEquals("blueOfficer", settings.team("blue").getCommanderClass());
    }

    @Test
    void alliedTeamsShareOneCommanderAndInvalidClassesDoNotSpawn()
    {
        ITeam red = team("red", "officer");
        ITeam ally = team("ally", "allyOfficer");
        Battleground ground = mock(Battleground.class);
        when(ground.teams()).thenReturn(List.of(red, ally));
        when(ground.team("red")).thenReturn(Optional.of(red));
        when(ground.team("ally")).thenReturn(Optional.of(ally));
        BotSettings settings = new BotSettings();
        settings.setCommanderBot(true);
        settings.team("red").setCommanderClass("missing");
        settings.team("ally").setCommanderClass("allyOfficer");
        BotBattle battle = new BotBattle(UUID.randomUUID(), ground, settings, Map.of());
        CommanderBots.keep(battle);
        assertEquals(1, battle.getSlots().size());
        assertEquals("ally", battle.getSlots().values().iterator().next().getTeam());
        settings.team("red").setCommanderClass("officer");
        CommanderBots.keep(battle);
        assertEquals(1, battle.getSlots().size(), "a living commander is retained when an ally adds a class");
    }

    @Test
    void deathDisruptsOnlyItsSideUntilDeadlineEvenAfterRespawn()
    {
        ITeam red = team("red", "officer");
        Battleground ground = mock(Battleground.class);
        when(ground.team("red")).thenReturn(Optional.of(red));
        BotSettings settings = new BotSettings();
        settings.setCommanderBot(true);
        BotBattle battle = new BotBattle(UUID.randomUUID(), ground, settings, Map.of());
        BotSlot commander = battle.newSlot("red", BotSlot.Source.COMMANDER, -1);
        CommanderBots.fallen(battle, commander, 100L);
        commander.setEntity(UUID.randomUUID());
        assertTrue(CommanderBots.disrupted(battle, 0, 499L));
        assertFalse(CommanderBots.disrupted(battle, 1, 101L));
        assertFalse(CommanderBots.disrupted(battle, 0, 500L));
        battle.getDisruptedUntil().clear();
        CommanderBots.fallen(battle, battle.newSlot("red", BotSlot.Source.INFANTRY, -1), 600L);
        assertFalse(CommanderBots.disrupted(battle, 0, 601L));
        commander.setRetired(true);
        CommanderBots.fallen(battle, commander, 700L);
        assertFalse(CommanderBots.disrupted(battle, 0, 701L), "administrative removal is not a commander kill");
        commander.setRetired(false);
        settings.setCommanderDisruptionSeconds(0);
        CommanderBots.fallen(battle, commander, 800L);
        assertFalse(CommanderBots.disrupted(battle, 0, 800L));
    }

    private static ITeam team(String name, String officer)
    {
        IPlayerClass playerClass = mock(IPlayerClass.class);
        when(playerClass.getShortName()).thenReturn(officer);
        ITeam team = mock(ITeam.class);
        when(team.getShortName()).thenReturn(name);
        doReturn(List.of(playerClass)).when(team).getClasses();
        return team;
    }
}
