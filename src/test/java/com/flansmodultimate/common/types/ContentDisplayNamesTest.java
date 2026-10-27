package com.flansmodultimate.common.types;

import com.flansmodultimate.common.teams.editor.EditedPlayerClass;
import com.flansmodultimate.common.teams.editor.EditedTeam;
import com.flansmodultimate.content.ContentPack;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentDisplayNamesTest
{
    @ParameterizedTest
    @EnumSource(value = EnumType.class, names = {"TEAM", "PLAYER_CLASS"}, mode = EnumSource.Mode.EXCLUDE)
    void everyOtherDefinitionKindHasATranslationKeyAndNameFallback(EnumType category)
    {
        InfoType definition = new NamedDefinition(category);
        String prefix = category.getIdentifier();
        if (category.isHasBlock())
            prefix = "block";
        else if (category.isHasItem())
            prefix = "item";
        String shortName = category.isHasItem() ? "sample_2" : "sample";
        var component = definition.getNameComponent();
        var contents = (TranslatableContents) component.getContents();
        assertEquals(prefix + ".flansmod." + shortName, contents.getKey());
        assertEquals("Fallback Name", contents.getFallback());
        assertEquals("Fallback Name", component.getString());
        assertEquals("Fallback Name", definition.getName());
    }

    @Test
    void namesFallBackWithoutTranslationsAndKeysDeriveFromOriginalShortNames()
    {
        PlayerClass legacy = playerClass("Name Default Soldier");
        assertEquals("Default Soldier", legacy.getNameComponent().getString());
        assertEquals("", legacy.getNameTranslationKey());
        PlayerClass translated = playerClass("Name Default Soldier", "ShortName Example_Soldier");
        var contents = (TranslatableContents) translated.getNameComponent().getContents();
        assertEquals("class.flansmod.example_soldier", contents.getKey());
        assertEquals("Default Soldier", contents.getFallback());
        assertEquals("Default Soldier", translated.getName());
    }

    @Test
    void registryAliasesDoNotChangeKeysAndTeamAndClassNamesRemainSeparate()
    {
        PlayerClass type = playerClass("Name Soldier", "ShortName Example_Soldier");
        type.uniqueShortName = "example_soldier_2";
        assertEquals("example_soldier_2", type.getShortName());
        assertEquals("class.flansmod.example_soldier", type.getNameTranslationKey());
        Team team = new Team();
        team.read(file(EnumType.TEAM, "Name Team", "ShortName Example_Soldier"));
        team.uniqueShortName = "example_soldier_3";
        assertEquals("team.flansmod.example_soldier", team.getNameTranslationKey());
    }

    @Test
    void theSameComponentFollowsResourceLanguageReloadWithoutChangingCanonicalData()
    {
        PlayerClass type = playerClass("Name Default Soldier", "ShortName Example_Soldier");
        var component = type.getNameComponent();
        Language previous = Language.getInstance();
        try
        {
            Language enabled = mock(Language.class);
            when(enabled.getOrDefault("class.flansmod.example_soldier", "Default Soldier")).thenReturn("Alternate Soldier");
            Language.inject(enabled);
            assertEquals("Alternate Soldier", component.getString());
            Language disabled = mock(Language.class);
            when(disabled.getOrDefault("class.flansmod.example_soldier", "Default Soldier")).thenReturn("Default Soldier");
            Language.inject(disabled);
            assertEquals("Default Soldier", component.getString());
            assertEquals("Default Soldier", type.getName());
        }
        finally
        {
            Language.inject(previous);
        }
    }

    @Test
    void editorChangesPreserveTranslationsUntilTheNameActuallyChanges()
    {
        Team team = new Team();
        team.read(file(EnumType.TEAM, "Name Default Team", "ShortName team"));
        PlayerClass type = playerClass("Name Default Soldier", "ShortName Example_Soldier");
        JsonObject unchanged = new JsonObject();
        unchanged.addProperty("name", "Default Team");
        assertEquals("team.flansmod.team", new EditedTeam("team", team, unchanged).getNameTranslationKey());
        unchanged.addProperty("name", "Default Soldier");
        assertEquals("class.flansmod.example_soldier", new EditedPlayerClass("soldier", type, unchanged).getNameTranslationKey());
        JsonObject rename = new JsonObject();
        rename.addProperty("name", "Builder's Name");
        assertEquals("Builder's Name", new EditedTeam("team", team, rename).getNameComponent().getString());
        assertEquals("", new EditedPlayerClass("soldier", type, rename).getNameTranslationKey());
        assertEquals("", new EditedTeam("custom:team", null, rename).getNameTranslationKey());
        assertEquals("", new EditedPlayerClass("custom:soldier", null, rename).getNameTranslationKey());
    }

    private static PlayerClass playerClass(String... lines)
    {
        PlayerClass type = new PlayerClass();
        type.read(file(EnumType.PLAYER_CLASS, lines));
        return type;
    }

    private static TypeFile file(EnumType type, String... lines)
    {
        return new TypeFile("display-name-test", type, new ContentPack("Display names", Path.of("build", "test-packs", "names")), List.of(lines));
    }

    private static final class NamedDefinition extends InfoType
    {
        private NamedDefinition(EnumType category)
        {
            type = category;
            name = "Fallback Name";
            originalShortName = "sample";
            uniqueShortName = "sample_2";
        }

        @Override
        public String getShortName()
        {
            return uniqueShortName;
        }
    }
}
