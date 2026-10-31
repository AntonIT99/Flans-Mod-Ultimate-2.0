package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.crew.CrewPost;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotSettingsTest
{
    @Test
    void aRoundWithoutSettingsHasNoBots()
    {
        assertFalse(BotSettings.load(new CompoundTag()).isEnabled());
    }

    @Test
    void settingsSurviveSavingAndLoading()
    {
        BotSettings settings = new BotSettings();
        settings.setMode(BotSettings.Mode.SHARE);
        settings.setTotal(90);
        settings.setNameplates(BotSettings.Nameplates.NEVER);
        settings.setScoreboard(BotSettings.Scoreboard.GROUPED);
        settings.setTickets(150);
        settings.setVehicleCaptureWeight(0.5F);
        BotSettings.TeamForce germany = settings.team("germany");
        germany.setCount(2);
        germany.setBudget(40);
        germany.setNameList("german_20th");
        germany.setCloneTab(3);
        germany.setCloneName("Wehrmacht");
        germany.getClassWeights().put("rifleman", 5);
        germany.getSquads().add(new BotSettings.SquadEntry("panzergrenadiers", 2, 6));
        UUID spawner = UUID.randomUUID();
        germany.getVehicles().add(new BotSettings.VehicleEntry("tiger", 2, 4, 90, 5, spawner, "grenadiers"));

        BotSettings loaded = BotSettings.load(settings.save());
        assertTrue(loaded.isEnabled());
        assertEquals(BotSettings.Mode.SHARE, loaded.getMode());
        assertEquals(90, loaded.getTotal());
        assertEquals(BotSettings.Nameplates.NEVER, loaded.getNameplates());
        assertEquals(BotSettings.Scoreboard.GROUPED, loaded.getScoreboard());
        assertEquals(150, loaded.getTickets());
        assertEquals(0.5F, loaded.getVehicleCaptureWeight());
        BotSettings.TeamForce force = loaded.team("germany");
        assertEquals(2, force.getCount());
        assertEquals(40, force.getBudget());
        assertEquals("german_20th", force.getNameList());
        assertTrue(force.usesClone());
        assertEquals(5, force.getClassWeights().get("rifleman"));
        assertEquals(List.of(new BotSettings.SquadEntry("panzergrenadiers", 2, 6)), force.getSquads());
        assertEquals(List.of(new BotSettings.VehicleEntry("tiger", 2, 4, 90, 5, spawner, "grenadiers")), force.getVehicles());
    }

    @Test
    void templateMembersAreReadAsClassCountPairs()
    {
        assertEquals(List.of(new SquadTemplate.Member("rifleman", 4), new SquadTemplate.Member("mg", 1), new SquadTemplate.Member("medic", 1)), BotSettingsText.parseMembers("rifleman:4, mg:1 medic"));
        SquadTemplate template = new SquadTemplate("squad", "Squad", null, null, List.of(new SquadTemplate.Member("officer", 1), new SquadTemplate.Member("rifleman", 2)));
        assertEquals(List.of("officer", "rifleman", "rifleman"), template.roster());
        assertEquals(SquadTemplate.Posture.BALANCED, template.posture());
    }

    @Test
    void formationSpotsAreDistinctAndPostsComeFirst()
    {
        Vec3 target = new Vec3(0D, 64D, 0D);
        Set<Vec3> spots = new HashSet<>();
        for (int i = 0; i < 8; i++)
        {
            Vec3 spot = BotFormation.spot(target, i, 10D, List.of());
            assertTrue(spot.distanceTo(target) <= 8D + 1.0E-9D, "spots stay within the radius");
            spots.add(spot);
        }
        assertEquals(8, spots.size());
        Vec3 post = new Vec3(30D, 70D, 5D);
        assertEquals(post, BotFormation.spot(target, 0, 10D, List.of(post)));
    }

    @Test
    void difficultyPlayerTicketsAndCloneEquipmentSurviveSavingAndLoading()
    {
        BotSettings settings = new BotSettings();
        settings.setDifficulty(BotSettings.Difficulty.CINEMATIC);
        settings.setPlayerTickets(false);
        settings.team("Germans").setKeepCloneEquipment(true);
        BotSettings loaded = BotSettings.load(settings.save());
        assertEquals(BotSettings.Difficulty.CINEMATIC, loaded.getDifficulty());
        assertFalse(loaded.isPlayerTickets());
        assertTrue(loaded.team("Germans").isKeepCloneEquipment());
    }

    @Test
    void difficultiesGoFromTheBestDownAndNoneMakesABotBetterThanItsEquipment()
    {
        BotSettings.Difficulty[] levels = BotSettings.Difficulty.values();
        assertEquals(BotSettings.Difficulty.ACE, levels[0]);
        assertFalse(levels[0].scalesStats());
        assertEquals(0, levels[0].skill().reactionTicks());
        for (int index = 1; index < levels.length; index++)
        {
            BotSettings.Difficulty better = levels[index - 1];
            BotSettings.Difficulty level = levels[index];
            assertTrue(level.skill().reactionTicks() > better.skill().reactionTicks(), level + " reacts slower than " + better);
            assertTrue(level.skill().settleTicks() >= better.skill().settleTicks(), level + " settles its aim no faster than " + better);
            assertTrue(level.skill().lead() <= better.skill().lead(), level + " leads no better than " + better);
            assertTrue(level.accuracy() <= 1F && level.aggro() <= 1F && level.health() <= 1F && level.delay() >= 1F, level + " stays within its equipment");
        }
        assertTrue(BotSettings.Difficulty.CINEMATIC.skill().nonLethal());
        assertFalse(BotSettings.Difficulty.RECRUIT.skill().nonLethal());
    }

    @Test
    void settingsSavedBeforeTheNewOptionsLetPlayersSpendTicketsAtTheDefaultDifficulty()
    {
        CompoundTag old = new BotSettings().save();
        old.remove("difficulty");
        old.remove("player_tickets");
        BotSettings loaded = BotSettings.load(old);
        assertEquals(BotSettings.Difficulty.ELITE, loaded.getDifficulty());
        assertTrue(loaded.isPlayerTickets());
    }

    @Test
    void rulesAndClassRolesSurviveSavingAndLoading()
    {
        BotSettings settings = new BotSettings();
        settings.setRadio(false);
        settings.setSuppression(false);
        settings.setRallyPoints(false);
        settings.setRetreatHealth(45);
        settings.team("Germans").getClassRoles().put("sani", BotSettings.ClassRole.MEDIC);
        BotSettings loaded = BotSettings.load(settings.save());
        assertFalse(loaded.isRadio());
        assertFalse(loaded.isSuppression());
        assertFalse(loaded.isRallyPoints());
        assertEquals(45, loaded.getRetreatHealth());
        assertEquals(BotSettings.ClassRole.MEDIC, loaded.team("Germans").getClassRoles().get("sani"));
    }

    @Test
    void presetsCarryForcesOverByTeamNameThenByPlace()
    {
        BotSettings settings = new BotSettings();
        settings.team("ger").setCount(12);
        settings.team("us").setCount(20);
        BotSettings same = BotPresets.carryOver(settings.save(), List.of("ger", "us"), List.of("us", "ger"));
        assertEquals(20, same.team("us").getCount(), "a team the rounds share keeps its own force");
        assertEquals(12, same.team("ger").getCount());
        BotSettings other = BotPresets.carryOver(settings.save(), List.of("ger", "us"), List.of("ita", "uk"));
        assertEquals(12, other.team("ita").getCount(), "other teams take the force in their place in round order");
        assertEquals(20, other.team("uk").getCount());
        assertFalse(other.getTeams().containsKey("ger"));
    }

    @Test
    void theMostSpecialisedRoleAKitAllowsWins()
    {
        assertEquals(BotSettings.ClassRole.MEDIC, BotRoles.role(true, true, true, true));
        assertEquals(BotSettings.ClassRole.ENGINEER, BotRoles.role(false, true, true, true));
        assertEquals(BotSettings.ClassRole.ANTI_TANK, BotRoles.role(false, false, true, true));
        assertEquals(BotSettings.ClassRole.SUPPORT, BotRoles.role(false, false, false, true));
        assertEquals(BotSettings.ClassRole.RIFLEMAN, BotRoles.role(false, false, false, false));
    }

    @Test
    void vehicleCrewsSurviveSavingAndLoading()
    {
        BotSettings settings = new BotSettings();
        settings.setVehicleCrews(true);
        BotSettings.TeamForce force = settings.team("germany");
        force.setCrewClass(CrewPost.DRIVER, "tank_crewman");
        force.setCrewClass(CrewPost.PASSENGER, BotSettings.TeamForce.NO_CREW);
        force.setCrewClass(CrewPost.GUNNER, " ");

        BotSettings loaded = BotSettings.load(settings.save());
        assertTrue(loaded.isVehicleCrews());
        assertEquals("tank_crewman", loaded.team("germany").crewClass(CrewPost.DRIVER));
        assertEquals("", loaded.team("germany").crewClass(CrewPost.GUNNER), "a blank class is picked by weight");
        assertEquals(BotSettings.TeamForce.NO_CREW, loaded.team("germany").crewClass(CrewPost.PASSENGER));
        assertFalse(BotSettings.load(new BotSettings().save()).isVehicleCrews(), "vehicles spawn empty unless crews are switched on");
    }
}
