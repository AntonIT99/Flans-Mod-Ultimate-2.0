package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.tactics.NpcTacticalProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BotSettingsTest
{
    @Test
    void ammunitionConsumptionDefaultsOffAndSurvivesSavingAndCopying()
    {
        assertFalse(new BotSettings().isConsumeAmmo());
        assertFalse(BotSettings.load(new CompoundTag()).isConsumeAmmo());
        BotSettings settings = new BotSettings();
        settings.setConsumeAmmo(true);
        BotSettings copy = BotSettings.load(settings.save());
        assertTrue(copy.isConsumeAmmo());
        copy.setConsumeAmmo(false);
        assertFalse(BotSettings.load(copy.save()).isConsumeAmmo());
        assertTrue(settings.isConsumeAmmo());
    }

    @Test
    void multipleCrewClassesSurviveSavesAndScopeInheritance()
    {
        BotSettings settings = new BotSettings();
        var force = settings.team("red");
        String planes = BotSettings.TeamForce.crewScope(FlanModelKind.PLANE);
        String bomber = BotSettings.TeamForce.crewDefinitionScope("bomber");
        force.setCrewClass(CrewPost.DRIVER, "driver|engineer");
        force.setCrewClass(planes, CrewPost.DRIVER, "pilot|ace");
        force.setCrewClass(bomber, CrewPost.GUNNER, "gunner|engineer");
        var loaded = BotSettings.load(settings.save()).team("red");
        assertEquals("driver|engineer", loaded.effectiveCrewClass("tank", FlanModelKind.VEHICLE, CrewPost.DRIVER));
        assertEquals("pilot|ace", loaded.effectiveCrewClass("bomber", FlanModelKind.PLANE, CrewPost.DRIVER));
        assertEquals("gunner|engineer", loaded.effectiveCrewClass("bomber", FlanModelKind.PLANE, CrewPost.GUNNER));
        loaded.setCrewClass(bomber, CrewPost.DRIVER, "");
        assertEquals("", loaded.effectiveCrewClass("bomber", FlanModelKind.PLANE, CrewPost.DRIVER));
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void wreckChanceDefaultsTo25AndPreservesSavedValues()
    {
        assertEquals(25, new BotSettings().getAbandonChance());
        assertEquals(25, BotSettings.load(new CompoundTag()).getAbandonChance());
        BotSettings settings = new BotSettings();
        settings.setAbandonChance(100);
        assertEquals(100, BotSettings.load(settings.save()).getAbandonChance());
    }

    @Test
    void repairableWreckOnlySurvivesItsOwnRunningRound()
    {
        UUID round = UUID.randomUUID();
        CompoundTag tag = new CompoundTag();
        assertFalse(BotTransports.staleWreck(tag, java.util.Optional.empty()));
        tag.putUUID(BotTransports.NBT_WRECK_ROUND, round);
        assertFalse(BotTransports.staleWreck(tag, java.util.Optional.of(round)));
        assertTrue(BotTransports.staleWreck(tag, java.util.Optional.of(UUID.randomUUID())));
        assertTrue(BotTransports.staleWreck(tag, java.util.Optional.empty()));
    }

    @Test
    void emplacementsDefaultOnForOldRoundsAndExplicitDisableSurvivesCopies()
    {
        BotSettings settings = new BotSettings();
        assertTrue(settings.isEmplacements());
        assertTrue(settings.isPlayerSeatDeployment());
        CompoundTag old = settings.save();
        old.remove("emplacements");
        old.remove("player_seat_deployment");
        assertTrue(BotSettings.load(old).isEmplacements());
        assertTrue(BotSettings.load(old).isPlayerSeatDeployment());
        settings.setEmplacements(false);
        settings.setPlayerSeatDeployment(false);
        assertFalse(BotSettings.load(settings.save()).isEmplacements());
        assertFalse(BotSettings.load(settings.save()).isPlayerSeatDeployment());
        assertFalse(BotPresets.carryOver(settings.save(), List.of("red"), List.of("blue")).isEmplacements());
    }

    @Test
    void vehicleRespawnDefaultAndOverridesSurviveSaveAndOldRoundsKeepTheirDelay()
    {
        BotSettings settings = new BotSettings();
        settings.setRespawnSeconds(7);
        settings.setVehicleRespawnSeconds(45);
        settings.team("red").getVehicles().add(new BotSettings.VehicleEntry("tank", 2, 0, 0, 1, null, ""));
        settings.team("red").getVehicles().add(new BotSettings.VehicleEntry("heavy", 1, 0, 120, 1, null, ""));
        BotSettings loaded = BotSettings.load(settings.save());
        assertEquals(7, TeamsBots.respawnSeconds(loaded, new BotSlot(1, "red", BotSlot.Source.INFANTRY, -1, true)));
        assertEquals(45, TeamsBots.respawnSeconds(loaded, new BotSlot(2, "red", BotSlot.Source.VEHICLE, 0, true)));
        assertEquals(120, TeamsBots.respawnSeconds(loaded, new BotSlot(3, "red", BotSlot.Source.VEHICLE, 1, true)));
        loaded.setVehicleRespawnSeconds(0);
        assertEquals(0, BotSettings.load(loaded.save()).getVehicleRespawnSeconds());
        CompoundTag old = settings.save();
        old.remove("vehicle_respawn_seconds");
        assertEquals(7, BotSettings.load(old).getVehicleRespawnSeconds());
        old.putInt("vehicle_respawn_seconds", -1);
        assertEquals(0, BotSettings.load(old).getVehicleRespawnSeconds());
    }

    @Test
    void physicalCommanderIsOptInAndSettingsTravelWithPresets()
    {
        BotSettings legacy = BotSettings.load(new CompoundTag());
        assertFalse(legacy.isCommanderBot());
        assertEquals(20, legacy.getCommanderDisruptionSeconds());
        BotSettings settings = new BotSettings();
        settings.setCommanderBot(true);
        settings.setCommanderDisruptionSeconds(37);
        settings.team("red").setCommanderClass("officer");
        BotSettings copy = BotPresets.carryOver(settings.save(), List.of("red"), List.of("blue"));
        assertTrue(copy.isCommanderBot());
        assertEquals(37, copy.getCommanderDisruptionSeconds());
        assertEquals("officer", copy.team("blue").getCommanderClass());
        settings.setCommanderDisruptionSeconds(0);
        assertEquals(0, BotSettings.load(settings.save()).getCommanderDisruptionSeconds());
    }

    @Test
    void commanderStaysBehindFrontWithoutGoingBehindHome()
    {
        Vec3 home = new Vec3(0D, 64D, 0D);
        assertEquals(home, CommanderBots.rear(home, new Vec3(20D, 70D, 0D)));
        assertEquals(new Vec3(52D, 64D, 0D), CommanderBots.rear(home, new Vec3(100D, 80D, 0D)));
        assertEquals(home, CommanderBots.rear(home, home));
    }

    @Test
    void vehicleToggleKeepsAllocationsAndDefaultsOnForExistingRounds()
    {
        BotSettings settings = new BotSettings();
        BotSettings.VehicleEntry vehicle = new BotSettings.VehicleEntry("tank", 2, 5, 15, 3, UUID.randomUUID(), "squad", List.of(1, 2));
        settings.team("red").getVehicles().add(vehicle);
        settings.setVehiclesEnabled(false);
        BotSettings loaded = BotSettings.load(settings.save());
        assertFalse(loaded.isVehiclesEnabled());
        assertTrue(loaded.isEnabled());
        assertEquals(List.of(vehicle), loaded.team("red").getVehicles());
        loaded.setVehiclesEnabled(true);
        assertTrue(BotSettings.load(loaded.save()).isVehiclesEnabled());
        assertEquals(List.of(vehicle), loaded.team("red").getVehicles());
        CompoundTag legacy = settings.save();
        legacy.remove("vehicles_enabled");
        assertTrue(BotSettings.load(legacy).isVehiclesEnabled());
        assertTrue(BotSettings.load(new CompoundTag()).isVehiclesEnabled());
    }

    @Test
    void battleBehaviourDefaultsToIndividualSettingsAndExplicitOverridesSurviveSaving()
    {
        assertEquals(NpcTacticalProfile.INHERIT, BotSettings.load(new CompoundTag()).getTacticalProfile());
        BotSettings settings = new BotSettings();
        settings.setTacticalProfile(NpcTacticalProfile.CAUTIOUS);
        assertEquals(NpcTacticalProfile.CAUTIOUS, BotSettings.load(settings.save()).getTacticalProfile());
    }

    @Test
    void vehiclePaintjobChoicesSurviveNbtAndTextAndLegacyAllocationsKeepDefault()
    {
        for (List<Integer> jobs : List.of(List.<Integer>of(), List.of(0, 3, 7)))
        {
            BotSettings.VehicleEntry entry = new BotSettings.VehicleEntry("tank", 2, 0, 10, 1, null, "", jobs);
            assertEquals(entry, BotSettings.VehicleEntry.load(entry.save()));
            assertEquals(List.of(entry), BotSettingsText.parseVehicles(BotSettingsText.formatVehicles(List.of(entry)), List.of()));
        }
        BotSettings.VehicleEntry legacy = new BotSettings.VehicleEntry("tank", 1, 0, 0, 1, null, "");
        CompoundTag old = legacy.save();
        old.remove("paintjobs");
        assertEquals(List.of(0), BotSettings.VehicleEntry.load(old).paintjobs());
    }

    @Test
    void aRoundWithoutSettingsHasNoBots()
    {
        assertFalse(BotSettings.load(new CompoundTag()).isEnabled());
    }

    @Test
    void regularIsTheDefaultButSavedDifficultiesArePreserved()
    {
        assertEquals(BotSettings.Difficulty.REGULAR, new BotSettings().getDifficulty());
        assertEquals(BotSettings.Difficulty.REGULAR, BotSettings.load(new CompoundTag()).getDifficulty());
        BotSettings saved = new BotSettings();
        saved.setDifficulty(BotSettings.Difficulty.ELITE);
        assertEquals(BotSettings.Difficulty.ELITE, BotSettings.load(saved.save()).getDifficulty());
        assertEquals(BotSettings.Difficulty.REGULAR, new Skirmish(UUID.randomUUID(), "test", net.minecraft.world.level.Level.OVERWORLD).getDifficulty());
        assertEquals(BotSettings.Difficulty.REGULAR, new War(UUID.randomUUID(), "test").getDifficulty());
    }

    @Test
    void classesWeighOneEachUntilOneOfTheTeamsHasAWeight()
    {
        BotSettings.TeamForce force = new BotSettings.TeamForce();
        List<String> classes = List.of("rifleman", "mg", "medic");
        assertEquals(Map.of("rifleman", 1, "mg", 1, "medic", 1), force.effectiveClassWeights(classes));
        // A weight for a class the team does not offer weighs nothing, as when bots are given classes.
        force.getClassWeights().put("sniper", 5);
        assertEquals(Map.of("rifleman", 1, "mg", 1, "medic", 1), force.effectiveClassWeights(classes));
        force.getClassWeights().put("rifleman", 3);
        force.getClassWeights().put("mg", 0);
        assertEquals(Map.of("rifleman", 3, "mg", 0, "medic", 0), force.effectiveClassWeights(classes));
        assertEquals(List.copyOf(classes), List.copyOf(force.effectiveClassWeights(classes).keySet()));
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
    void difficultyAndCloneEquipmentSurviveSavingAndLoading()
    {
        BotSettings settings = new BotSettings();
        settings.setDifficulty(BotSettings.Difficulty.TRAINEE);
        settings.team("Germans").setKeepCloneEquipment(true);
        BotSettings loaded = BotSettings.load(settings.save());
        assertEquals(BotSettings.Difficulty.TRAINEE, loaded.getDifficulty());
        assertTrue(loaded.team("Germans").isKeepCloneEquipment());
    }

    @Test
    void difficultiesGoFromTheEasiestUpAndNoneMakesABotBetterThanItsEquipment()
    {
        List<BotSettings.Difficulty> levels = BotSettings.Difficulty.presets();
        assertEquals(BotSettings.Difficulty.TRAINEE, levels.get(0));
        assertEquals(BotSettings.Difficulty.ACE, levels.get(levels.size() - 1));
        assertFalse(BotSettings.Difficulty.ACE.scalesStats());
        assertEquals(0, BotSettings.Difficulty.ACE.skill().reactionTicks());
        for (int index = 1; index < levels.size(); index++)
        {
            BotSettings.Difficulty better = levels.get(index);
            BotSettings.Difficulty level = levels.get(index - 1);
            assertTrue(level.skill().reactionTicks() > better.skill().reactionTicks(), level + " reacts slower than " + better);
            assertTrue(level.skill().settleTicks() >= better.skill().settleTicks(), level + " settles its aim no faster than " + better);
            assertTrue(level.skill().lead() <= better.skill().lead(), level + " leads no better than " + better);
            assertTrue(level.accuracy() <= 1F && level.aggro() <= 1F, level + " stays within its equipment");
        }
        for (BotSettings.Difficulty level : levels)
        {
            assertFalse(level.skill().nonLethal(), level + " leaves sparing players to Cinematic mode");
            assertEquals(level.compareTo(BotSettings.Difficulty.REGULAR) >= 0, level.skill().playerDamage() >= 1F, level + " softens its hits on players only below Regular");
        }
    }

    @Test
    void customDifficultyKeepsItsValuesWithinBoundsAndRetrainsWhenTheyChange()
    {
        BotSettings settings = new BotSettings();
        assertEquals(BotSettings.Difficulty.REGULAR.training(null), settings.getCustomTraining(), "custom values start as those of Regular");
        BotTraining custom = BotTraining.of(60, 80, 12, 30, 6.5F, 40, 50, true);
        settings.setDifficulty(BotSettings.Difficulty.CUSTOM);
        settings.setCustomTraining(custom);
        BotSettings loaded = BotSettings.load(settings.save());
        assertEquals(BotSettings.Difficulty.CUSTOM, loaded.getDifficulty());
        assertEquals(custom, loaded.training());
        assertEquals("60 80 12 30 6.5 40 50 true", custom.commandArguments());
        String trained = loaded.trainingId();
        loaded.setCustomTraining(BotTraining.of(60, 80, 12, 30, 6.5F, 40, 50, false));
        assertNotEquals(trained, loaded.trainingId(), "other custom values train bots again");
        BotTraining wild = BotTraining.of(500, 0, -5, 9999, 90F, 300, 300, false);
        assertEquals(100, wild.accuracyPercent());
        assertEquals(1, wild.aggroPercent());
        assertEquals(0, wild.skill().reactionTicks());
        assertEquals(BotTraining.MOST_SETTLE_TICKS, wild.skill().settleTicks());
        assertEquals(BotTraining.MOST_ERROR, wild.skill().startError());
        assertEquals(100, wild.leadPercent());
        assertEquals(100, wild.playerDamagePercent(), "no custom value makes a bot better than its equipment");
    }

    @Test
    void legacyPlayerTicketSettingsAreIgnored()
    {
        CompoundTag old = new BotSettings().save();
        old.remove("difficulty");
        old.putBoolean("player_tickets", true);
        BotSettings loaded = BotSettings.load(old);
        assertEquals(BotSettings.Difficulty.REGULAR, loaded.getDifficulty());
        assertFalse(loaded.save().contains("player_tickets"));
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
    void aSingleBandageIsFirstAidRatherThanAMedicKit()
    {
        assertFalse(BotRoles.dedicatedMedicalTool("44_bandage", 1));
        assertTrue(BotRoles.dedicatedMedicalTool("44_bandage", 4));
        assertTrue(BotRoles.dedicatedMedicalTool("ww2medkit", 1));
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
        assertTrue(new BotSettings().isVehicleCrews(), "vehicles spawn with crews by default");
        CompoundTag legacy = settings.save();
        legacy.remove("vehicle_crews");
        assertTrue(BotSettings.load(legacy).isVehicleCrews(), "missing crew settings use the default");
        settings.setVehicleCrews(false);
        assertFalse(BotSettings.load(settings.save()).isVehicleCrews(), "explicitly disabled crews stay disabled");
    }

    @Test
    void crewOverridesResolvePerSeatAndSurviveSaving()
    {
        BotSettings settings = new BotSettings();
        BotSettings.TeamForce force = settings.team("blue");
        String planes = BotSettings.TeamForce.crewScope(FlanModelKind.PLANE);
        String bomber = BotSettings.TeamForce.crewDefinitionScope("bomber");
        force.setCrewClass(CrewPost.DRIVER, "crewman");
        force.setCrewClass(CrewPost.GUNNER, "gunner");
        force.setCrewClass(CrewPost.PASSENGER, "infantry");
        force.setCrewClass(planes, CrewPost.DRIVER, "pilot");
        force.setCrewClass(planes, CrewPost.PASSENGER, BotSettings.TeamForce.NO_CREW);
        force.setCrewClass(bomber, CrewPost.GUNNER, "air_gunner");
        force.setCrewClass(bomber, CrewPost.PASSENGER, "");

        BotSettings.TeamForce loaded = BotSettings.load(settings.save()).team("blue");
        assertEquals("pilot", loaded.effectiveCrewClass("bomber", FlanModelKind.PLANE, CrewPost.DRIVER));
        assertEquals("air_gunner", loaded.effectiveCrewClass("bomber", FlanModelKind.PLANE, CrewPost.GUNNER));
        assertEquals("", loaded.effectiveCrewClass("bomber", FlanModelKind.PLANE, CrewPost.PASSENGER), "explicit any overrides no crew");
        assertEquals("gunner", loaded.effectiveCrewClass("fighter", FlanModelKind.PLANE, CrewPost.GUNNER));
        assertEquals(BotSettings.TeamForce.NO_CREW, loaded.effectiveCrewClass("fighter", FlanModelKind.PLANE, CrewPost.PASSENGER));
        assertEquals("crewman", loaded.effectiveCrewClass("tank", FlanModelKind.VEHICLE, CrewPost.DRIVER));
        assertEquals("crewman", loaded.effectiveCrewClass("unknown", null, CrewPost.DRIVER));

        loaded.setCrewClass(bomber, CrewPost.GUNNER, null);
        assertEquals("gunner", loaded.effectiveCrewClass("bomber", FlanModelKind.PLANE, CrewPost.GUNNER));
        loaded.setCrewClass(bomber, CrewPost.PASSENGER, null);
        assertFalse(loaded.crewScopes().contains(bomber), "removing the last seat removes the definition override");
    }

    @Test
    void crewDraftsCopyOverridesWithoutSharingMutableState()
    {
        BotSettings.TeamForce force = new BotSettings.TeamForce();
        String planes = BotSettings.TeamForce.crewScope(FlanModelKind.PLANE);
        force.setCrewClass(CrewPost.DRIVER, "crewman");
        force.setCrewClass(planes, CrewPost.DRIVER, "pilot");
        BotSettings.TeamForce draft = new BotSettings.TeamForce();
        draft.copyCrewFrom(force);
        draft.setCrewClass(CrewPost.DRIVER, "replacement");
        draft.setCrewClass(planes, CrewPost.DRIVER, "ace");
        assertEquals("crewman", force.crewClass(CrewPost.DRIVER));
        assertEquals("pilot", force.crewClass(planes, CrewPost.DRIVER));

        force.copyCrewFrom(draft);
        draft.setCrewClass(planes, CrewPost.DRIVER, null);
        assertEquals("replacement", force.crewClass(CrewPost.DRIVER));
        assertEquals("ace", force.crewClass(planes, CrewPost.DRIVER));
        force.copyCrewFrom(force);
        assertEquals("ace", force.crewClass(planes, CrewPost.DRIVER));
    }
}
