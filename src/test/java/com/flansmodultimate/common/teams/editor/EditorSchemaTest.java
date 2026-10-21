package com.flansmodultimate.common.teams.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditorSchemaTest
{
    private static TeamEditorStore.Document document(String json)
    {
        return new TeamEditorStore.Document(new JsonObject(), JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void validatesExplicitEmptyInheritanceOffhandAndCustomItemData()
    {
        assertDoesNotThrow(() -> EditorSchema.validate(document("""
            {"custom:rifleman":{"name":"Rifleman","armour":{"head":"empty","chest":"inherit"},
            "items":{"40":{"item":"minecraft:shield","count":1,"nbt":"{Damage:12}"}},
            "skins":["flansmod:textures/skins/soldier.png"],"classLimit":-1,"classLimitPercent":30.5}}
            """)));
    }

    @Test
    void rejectsTyposInvalidSlotsCountsAndNonFiniteLimits()
    {
        for (String patch : new String[]{"{\"nmae\":\"x\"}", "{\"items\":{\"36\":{\"item\":\"minecraft:stone\"}}}", "{\"items\":{\"0\":{\"item\":\"minecraft:stone\",\"count\":1000}}}",
            "{\"classLimitPercent\":101}", "{\"classLimit\":0.5}", "{\"skins\":[\"Bad Texture\"]}", "{\"disabled\":\"false\"}"})
        {
            TeamEditorStore.Document invalid = document("{\"custom:rifleman\":" + patch + "}");
            assertThrows(IllegalArgumentException.class, () -> EditorSchema.validate(invalid), patch);
        }
    }

    private static TeamEditorStore.Document teamDocument(String flag)
    {
        JsonObject team = new JsonObject();
        team.addProperty("victoryFlag", flag);
        JsonObject teams = new JsonObject();
        teams.add("custom:red", team);
        return new TeamEditorStore.Document(teams, new JsonObject());
    }

    @Test
    void acceptsATeamFlagAsTextureOrWebAddressOnly()
    {
        for (String flag : new String[]{"", "flansmod:textures/skins/flag_red.png", "https://example.com/flags/red.png", "HTTP://cdn.example.org/red?size=large"})
            assertDoesNotThrow(() -> EditorSchema.validate(teamDocument(flag)), flag);
        for (String flag : new String[]{"Bad Texture", "https://", "https://user:secret@example.com/red.png", "https://exa mple.com/red.png", "https://example.com/" + "a".repeat(1100)})
        {
            TeamEditorStore.Document invalid = teamDocument(flag);
            assertThrows(IllegalArgumentException.class, () -> EditorSchema.validate(invalid), flag);
        }
    }

    @Test
    void patchingOneArmourSlotKeepsOtherOverridesAndDoesNotFreezeUnchangedFields()
    {
        JsonObject before = JsonParser.parseString("{\"name\":\"Original\",\"armour\":{\"head\":\"inherit\",\"chest\":\"empty\"}}").getAsJsonObject();
        JsonObject after = before.deepCopy();
        after.getAsJsonObject("armour").addProperty("head", "empty");
        JsonObject changes = EditorDescriptions.changes(before, after);
        assertFalse(changes.has("name"));
        JsonObject saved = JsonParser.parseString("{\"armour\":{\"chest\":\"empty\"}}").getAsJsonObject();
        EditorDescriptions.merge(saved, changes);
        assertEquals("empty", saved.getAsJsonObject("armour").get("head").getAsString());
        assertEquals("empty", saved.getAsJsonObject("armour").get("chest").getAsString());
        assertEquals("inherit", before.getAsJsonObject("armour").get("head").getAsString());
    }
}
