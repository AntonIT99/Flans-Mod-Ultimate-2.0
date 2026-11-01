package com.flansmodultimate.common.types;

import com.flansmodultimate.common.teams.editor.EditedTeam;
import com.flansmodultimate.content.ContentPack;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamClassContributionTest
{
    @Test
    void contributionsResolveByPackAndRespectEditorRosters()
    {
        ContentPack base = new ContentPack("Extension Base", Path.of("build/test-packs/extension-base"));
        ContentPack addon = new ContentPack("Extension Addon", Path.of("build/test-packs/extension-addon"));
        PlayerClass first = new PlayerClass();
        first.load(new TypeFile("extensionFirst", EnumType.PLAYER_CLASS, base, List.of("ShortName extensionFirst")));
        PlayerClass extra = new PlayerClass();
        extra.load(new TypeFile("extensionExtra", EnumType.PLAYER_CLASS, addon,
            List.of("ShortName extensionExtra", "AddToTeam extension+base:extensionTeam", "AddToTeam EXTENSION+BASE:EXTENSIONTEAM", "AddToTeam missing:extensionTeam", "AddToTeam extensionTeam")));
        Team team = new Team();
        team.load(new TypeFile("extensionTeam", EnumType.TEAM, base, List.of("ShortName extensionTeam", "AddClass extensionFirst")));
        Team other = new Team();
        other.load(new TypeFile("extensionTeam", EnumType.TEAM, addon, List.of("ShortName extensionTeam")));
        Team.resolveClassContributions(List.of(team, other), List.of(first, extra));
        assertEquals(List.of(first, extra), team.getClasses());
        assertTrue(other.getClasses().isEmpty());
        assertEquals(team.getClasses(), new EditedTeam(team.getShortName(), team, new JsonObject()).getClasses());
        JsonObject patch = new JsonObject();
        patch.add("classes", new JsonArray());
        assertTrue(new EditedTeam(team.getShortName(), team, patch).getClasses().isEmpty());
        Team.resolveClassContributions(List.of(team, other), List.of(first));
        assertEquals(List.of(first), team.getClasses());
    }
}
