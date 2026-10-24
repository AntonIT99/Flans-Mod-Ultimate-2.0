package com.wolffsmod.npcs.names;

import com.google.gson.Gson;
import com.wolffsmod.npcs.platform.NpcPlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class SoldierNameAssociationsTest
{
    @Test
    void runtimeNormalizedIdsResolveMixedCaseListsGenderOverridesAndDisabledClasses()
    {
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("WolffStarWars_RepublicStandardP1", "wolffsmodnpcs:clone_ct"),
            Map.of("WolffStarWars_RepublicStandardP1Commander", "wolffsmodnpcs:clone_cc", "DisabledClass", ""), Map.of("WolffStarWars_RepublicStandardP1", "mixed"),
            Map.of("WolffStarWars_RepublicStandardP1Commander", "female"));
        String team = com.flansmodultimate.util.ResourceUtils.sanitize("WolffStarWars_RepublicStandardP1");
        String playerClass = com.flansmodultimate.util.ResourceUtils.sanitize("WolffStarWars_RepublicStandardP1Commander");
        assertEquals(47, associations.find(team, playerClass).orElseThrow());
        assertEquals(46, associations.find(team, "ordinary").orElseThrow());
        assertEquals(2, associations.gender(team, playerClass));
        assertEquals(0, associations.gender(team, "ordinary"));
        assertTrue(associations.find(team, "disabledclass").isEmpty());
        Map<String, String> duplicate = Map.of("Team", "wolffsmodnpcs:german", "team", "wolffsmodnpcs:british");
        Map<String, String> empty = Map.of();
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameAssociations(duplicate, empty));
        Map<String, String> duplicateGenders = Map.of("Class", "male", "class", "female");
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameAssociations(empty, empty, empty, duplicateGenders));
    }

    @Test
    void genderDefaultsToMaleAndClassOverridesTeamWithoutChangingListInheritance()
    {
        SoldierNameAssociations defaults = new SoldierNameAssociations(Map.of(), Map.of());
        assertEquals(1, defaults.gender("team", "class"));
        SoldierNameAssociations configured = new Gson().fromJson("""
            {"teams":{"team":"wolffsmodnpcs:french"},"teamGenders":{"team":"mixed"},
             "classGenders":{"women":"female","men":"male"}}
            """, SoldierNameAssociations.class);
        assertEquals(0, configured.gender("team", "ordinary"));
        assertEquals(2, configured.gender("team", "women"));
        assertEquals(1, configured.gender("team", "men"));
        assertEquals(70, configured.find("team", "women").orElseThrow());
        Map<String, String> invalid = Map.of("team", "unknown");
        Map<String, String> empty = Map.of();
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameAssociations(empty, empty, invalid, empty));
    }

    @TempDir
    Path directory;

    @Test
    void classWinsAndEmptyClassDisablesTeamInheritance()
    {
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("team", "wolffsmodnpcs:german_20th"), Map.of("override", "wolffsmodnpcs:british_20th", "disabled", ""));
        assertEquals(14, associations.find("team", "ordinary").orElseThrow());
        assertEquals(15, associations.find("team", "override").orElseThrow());
        assertEquals(15, associations.find("", "override").orElseThrow());
        assertTrue(associations.find("team", "disabled").isEmpty());
        assertTrue(associations.find("unknown", "unknown").isEmpty());
        assertEquals(14, associations.find("TEAM", "ordinary").orElseThrow());
    }

    @Test
    void forceUserDefaultsFollowTheClassRank() throws Exception
    {
        SoldierNameAssociations defaults = SoldierNameAssociations.read(directory.resolve("force-names.json"));
        assertEquals(71, defaults.find("WolffStarWars_JediOrder", "WolffStarWars_JediKnightBlue").orElseThrow());
        assertEquals(72, defaults.find("WolffStarWars_JediOrder", "WolffStarWars_JediMasterPurple").orElseThrow());
        assertEquals(73, defaults.find("WolffStarWars_JediOrder", "WolffStarWars_JediTempleGuardYellow").orElseThrow());
        assertEquals(74, defaults.find("WolffStarWars_JediOrder", "WolffStarWars_JediWayfarerWhite").orElseThrow());
        assertEquals(75, defaults.find("WolffStarWars_SithOrder", "WolffStarWars_SithApprentice").orElseThrow());
        assertEquals(79, defaults.find("WolffStarWars_SithOrder", "WolffStarWars_SithLord").orElseThrow());
    }

    @Test
    void missingConfigCopiesDefaultsOnceAndUserEditsReplaceThem() throws Exception
    {
        Path file = directory.resolve("nested/soldier-names.json");
        SoldierNameAssociations defaults = SoldierNameAssociations.read(file);
        assertEquals(38, defaults.teams().size());
        assertEquals(70, defaults.find("gign", "gignAssault1").orElseThrow());
        assertEquals(0, defaults.find("WolffRome_Empire", "WolffRome_LegionaryHamata").orElseThrow());
        assertEquals(0, defaults.find("WolffRome_Republic", "WolffRome_Hastati").orElseThrow());
        assertEquals(96, defaults.classes().size());
        assertEquals(10, defaults.find("ksk", "kskAssault1").orElseThrow());
        assertTrue(defaults.find("", "kskAssault1").isEmpty());
        assertEquals(54, defaults.find("", "WolffStarWars_Republic501P1Rex").orElseThrow());
        assertEquals(55, defaults.find("", "WolffStarWars_Republic212P1Cody").orElseThrow());
        assertEquals(48, defaults.find("", "WolffStarWars_RepublicStandardP1ARCCaptain").orElseThrow());
        assertEquals(56, defaults.find("WolffStarWars_Imperial", "WolffStarWars_ImperialStormtrooper").orElseThrow());
        assertEquals(56, defaults.find("WolffStarWars_Imperial", "WolffStarWars_ImperialShadowtrooper").orElseThrow());
        assertEquals(63, defaults.find("WolffStarWars_SeparatistStandard", "WolffStarWars_SeparatistStandardBattleDroid").orElseThrow());
        assertEquals(64, defaults.find("WolffStarWars_SeparatistStandard", "WolffStarWars_SeparatistStandardCommander").orElseThrow());
        assertEquals(69, defaults.find("WolffStarWars_SeparatistGeonosis", "WolffStarWars_SeparatistGeonosisDroideka").orElseThrow());
        assertEquals(44, defaults.find("W44_SovietArmy", "W44_SovietArmyRifleman").orElseThrow());
        String edited = "{\"teams\":{\"myTeam\":\"wolffsmodnpcs:japanese_20th\"},\"classes\":{}}";
        Files.writeString(file, edited);
        SoldierNameAssociations custom = SoldierNameAssociations.read(file);
        assertEquals(45, custom.find("myTeam", "any").orElseThrow());
        assertTrue(custom.find("W44_SovietArmy", "any").isEmpty());
        assertEquals(edited, Files.readString(file));
        Map<String, String> immutableTeams = custom.teams();
        assertThrows(UnsupportedOperationException.class, () -> immutableTeams.put("extra", "wolffsmodnpcs:german"));
    }

    @Test
    void invalidConfigIsPreservedAndNextServerCannotReusePreviousAssociations() throws Exception
    {
        Path file = directory.resolve("wolffsmodnpcs/soldier-names.json");
        try (var platform = mockStatic(NpcPlatform.class))
        {
            platform.when(NpcPlatform::configDir).thenReturn(directory);
            SoldierNameAssociations.load();
            assertFalse(SoldierNameAssociations.current().classes().isEmpty());
            for (String invalid : new String[]{"null", "{", "{\"teams\":{\"team\":\"wolffsmodnpcs:missing\"}}", "{\"classes\":{\"class\":null}}"})
            {
                Files.writeString(file, invalid);
                SoldierNameAssociations.load();
                assertTrue(SoldierNameAssociations.current().teams().isEmpty());
                assertTrue(SoldierNameAssociations.current().classes().isEmpty());
                assertEquals(invalid, Files.readString(file));
            }
        }
    }

    @Test
    void everyBundledAssociationNamesAnExistingSourceDefinitionAndNameList() throws Exception
    {
        SoldierNameAssociations defaults;
        try (var stream = SoldierNameAssociations.class.getResourceAsStream("/data/wolffsmodnpcs/soldier-names.json"))
        {
            assertNotNull(stream);
            defaults = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), SoldierNameAssociations.class);
        }
        Set<String> teams = new HashSet<>();
        Set<String> classes = new HashSet<>();
        try (var paths = Files.walk(Path.of("src")))
        {
            for (Path path : paths.filter(path -> path.toString().endsWith(".txt") && (path.getParent().endsWith("teams") || path.getParent().endsWith("classes"))).toList())
            {
                Set<String> target = path.getParent().endsWith("teams") ? teams : classes;
                try (var lines = Files.lines(path))
                {
                    lines.filter(line -> line.startsWith("ShortName ")).map(line -> line.substring("ShortName ".length()).strip().toLowerCase(java.util.Locale.ROOT)).forEach(target::add);
                }
            }
        }
        assertTrue(teams.containsAll(defaults.teams().keySet()));
        assertTrue(classes.containsAll(defaults.classes().keySet()));
        defaults.teams().values().forEach(id -> assertTrue(SoldierNames.acceptsAssociation(id), id));
        defaults.classes().values().forEach(id -> assertTrue(SoldierNames.acceptsAssociation(id), id));
    }
}
