package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.api.teams.ITeamsLiveRound;
import com.flansmodultimate.api.teams.ITeamsRound;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotRespawnBudgetTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void objectiveModesIgnoreEvenExhaustedSavedBotTickets()
    {
        for (String mode : new String[]{"con", "dom", "aslt"})
        {
            BotBattle battle = battle(mode);
            BotSlot slot = battle.newSlot("red", BotSlot.Source.INFANTRY, 0);
            slot.setSpawns(1);
            battle.getTickets().put(0, 0);
            assertTrue(BotPopulation.canSpawn(battle, slot), mode);
            BotPopulation.charge(battle, slot);
            assertEquals(0, battle.getTickets().get(0), "ignored tickets must not be charged");
            assertEquals(1, battle.getInfantrySpawns().get("red"), "unit budgets still count spawns");
            battle.getSettings().team("red").setBudget(1);
            assertFalse(BotPopulation.canSpawn(battle, slot), "explicit force budgets still apply");
        }
    }

    @Test
    void otherModesAndStandaloneBattlesKeepBotOnlyRespawnLimits()
    {
        for (String mode : new String[]{"tdm", "ctf", "dm", "zom", ""})
        {
            BotBattle battle = battle(mode);
            BotSlot slot = battle.newSlot("red", BotSlot.Source.INFANTRY, 0);
            slot.setSpawns(1);
            assertTrue(BotPopulation.canSpawn(battle, slot), mode);
            BotPopulation.charge(battle, slot);
            assertEquals(0, battle.getTickets().get(0), mode);
            assertFalse(BotPopulation.canSpawn(battle, slot), mode);
            battle.getSettings().setTickets(0);
            assertTrue(BotPopulation.canSpawn(battle, slot), "zero remains unlimited");
        }
    }

    private static BotBattle battle(String mode)
    {
        Battleground ground = mock(Battleground.class);
        ITeam red = mock(ITeam.class);
        when(ground.team("red")).thenReturn(Optional.of(red));
        when(ground.sideOf(red)).thenReturn(0);
        if (!mode.isEmpty())
        {
            ITeamsLiveRound live = mock(ITeamsLiveRound.class);
            ITeamsRound round = mock(ITeamsRound.class);
            when(ground.round()).thenReturn(Optional.of(live));
            when(live.getRound()).thenReturn(round);
            when(round.getGameTypeId()).thenReturn(mode);
        }
        BotSettings settings = new BotSettings();
        settings.setTickets(1);
        return new BotBattle(UUID.randomUUID(), ground, settings, Map.of());
    }
}
