package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotInfantryClassLimitsTest
{
    @Test
    void capsAreIndependentAndSurviveSavingAndPresetTeamRemapping()
    {
        BotSettings settings = new BotSettings();
        assertTrue(BotSettings.load(new CompoundTag()).team("red").getClassLimits().isEmpty());
        BotSettings.TeamForce force = settings.team("red");
        force.getClassLimits().put("medic", new BotSettings.ClassLimit(2, 15F));
        force.getClassLimits().put("sniper", new BotSettings.ClassLimit(-1, 12.5F));
        force.getClassWeights().put("medic", 4);
        BotSettings copied = BotPresets.carryOver(settings.save(), List.of("red"), List.of("blue"));
        assertEquals(force.getClassLimits(), copied.team("blue").getClassLimits());
        assertEquals(Map.of("medic", 4), copied.team("blue").getClassWeights());
        assertTrue(copied.team("red").getClassLimits().isEmpty());
        assertEquals(1, force.classCapacity("medic", 1));
        assertEquals(2, force.classCapacity("medic", 100));
        assertEquals(2, force.classCapacity("sniper", 9));
        assertEquals(Integer.MAX_VALUE, force.classCapacity("rifleman", 100));
        assertEquals(0, new BotSettings.ClassLimit(-1, 0F).capacity(100));
        assertEquals(0, new BotSettings.ClassLimit(0, -1F).capacity(100));
        assertEquals(BotSettings.ClassLimit.UNLIMITED, new BotSettings.ClassLimit(-50, Float.NaN));
        assertEquals(100F, new BotSettings.ClassLimit(-1, 200F).percent());
    }

    @Test
    void reservationsRespectBothCapsWithoutCountingExplicitSquadsOrOtherTeams()
    {
        Fixture fixture = new Fixture();
        fixture.force.setCount(10);
        fixture.force.getClassWeights().putAll(Map.of("medic", 100, "sniper", 100, "rifleman", 1));
        fixture.force.getClassLimits().put("medic", new BotSettings.ClassLimit(-1, 10F));
        fixture.force.getClassLimits().put("sniper", new BotSettings.ClassLimit(2, 25F));
        BotSlot explicit = fixture.battle.newSlot("red", BotSlot.Source.SQUAD, 0);
        explicit.setRole("medic");
        BotSlot otherTeam = fixture.battle.newSlot("blue", BotSlot.Source.INFANTRY, -1);
        otherTeam.setRole("medic");
        fixture.upkeep();
        assertEquals(Map.of("medic", 1L, "sniper", 2L, "rifleman", 7L), fixture.allocations());
        assertFalse(explicit.isRetired());
        assertFalse(otherTeam.isRetired());
        int slots = fixture.battle.getSlots().size();
        fixture.upkeep();
        assertEquals(slots, fixture.battle.getSlots().size(), "Pending respawns must retain their class reservations");
    }

    @Test
    void fullWeightedClassesLeaveUnfilledCapacityAndRecoverWhenAnOverrideIsCleared()
    {
        Fixture fixture = new Fixture();
        fixture.force.setCount(10);
        fixture.force.getClassWeights().put("medic", 1);
        fixture.force.getClassLimits().put("medic", new BotSettings.ClassLimit(2, -1F));
        fixture.upkeep();
        assertEquals(Map.of("medic", 2L), fixture.allocations());
        int slots = fixture.battle.getSlots().size();
        fixture.upkeep();
        assertEquals(slots, fixture.battle.getSlots().size());
        fixture.force.getClassLimits().clear();
        fixture.upkeep();
        assertEquals(Map.of("medic", 10L), fixture.allocations());
    }

    @Test
    void loweringCapsAndShrinkingTheTargetRetiresExcessAndRefillsWithAllowedClasses()
    {
        Fixture fixture = new Fixture();
        fixture.force.setCount(10);
        fixture.upkeep();
        fixture.force.getClassLimits().put("medic", new BotSettings.ClassLimit(0, -1F));
        fixture.force.getClassLimits().put("sniper", new BotSettings.ClassLimit(-1, 10F));
        fixture.upkeep();
        assertEquals(Map.of("rifleman", 9L, "sniper", 1L), fixture.allocations());
        fixture.force.setCount(1);
        fixture.force.getClassLimits().put("sniper", new BotSettings.ClassLimit(0, -1F));
        fixture.upkeep();
        assertEquals(Map.of("rifleman", 1L), fixture.allocations());
        fixture.force.getClassLimits().put("rifleman", new BotSettings.ClassLimit(0, -1F));
        fixture.upkeep();
        assertTrue(fixture.allocations().isEmpty());
        assertTrue(fixture.battle.getSlots().values().stream().filter(BotSlot::isRetired).noneMatch(slot -> BotPopulation.canSpawn(fixture.battle, slot)));
    }

    private static final class Fixture
    {
        private final BotSettings settings = new BotSettings();
        private final BotSettings.TeamForce force = settings.team("red");
        private final BotBattle battle;
        private final MinecraftServer server = mock(MinecraftServer.class);

        private Fixture()
        {
            ITeam team = mock(ITeam.class);
            when(team.getShortName()).thenReturn("red");
            List<IPlayerClass> classes = new ArrayList<>();
            for (String name : List.of("rifleman", "medic", "sniper"))
            {
                IPlayerClass playerClass = mock(IPlayerClass.class);
                when(playerClass.getShortName()).thenReturn(name);
                classes.add(playerClass);
            }
            doReturn(classes).when(team).getClasses();
            Battleground ground = mock(Battleground.class);
            when(ground.teams()).thenReturn(List.of(team));
            battle = new BotBattle(UUID.randomUUID(), ground, settings, Map.of());
            PlayerList players = mock(PlayerList.class);
            when(server.getPlayerList()).thenReturn(players);
            when(players.getPlayers()).thenReturn(List.of());
        }

        private void upkeep()
        {
            BotPopulation.upkeep(server, battle, 100L, new Random(1));
        }

        private Map<String, Long> allocations()
        {
            Map<String, Long> counts = new HashMap<>();
            battle.getSlots().values().stream().filter(slot -> !slot.isRetired() && slot.getSource() == BotSlot.Source.INFANTRY && "red".equals(slot.getTeam()))
                .forEach(slot -> counts.merge(slot.getRole(), 1L, Long::sum));
            return counts;
        }
    }
}
