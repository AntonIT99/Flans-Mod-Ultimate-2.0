package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.item.ItemStackData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditorCatalogTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @AfterEach
    void clear()
    {
        EditorCatalog.clearClient();
        EditorCatalog.clearServer();
    }

    @Test
    void itemTemplatesPreserveDamageNamesAndCustomNbt()
    {
        ItemStack original = new ItemStack(Items.IRON_SWORD);
        original.setDamageValue(17);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Officer's sword"));
        ItemStackData.update(original, tag -> tag.putString("custom_mod_data", "keep me"));
        JsonObject saved = EditorStacks.save(original);
        ItemStack restored = EditorStacks.load(saved);
        assertEquals(original.getCount(), restored.getCount());
        assertTrue(ItemStackData.isSameItemSameData(original, restored));
    }

    @Test
    void clientDefinitionsExposeCustomClassesToTheNpcApiWithoutChangingPackRegistries()
    {
        JsonObject classes = JsonParser.parseString("""
            {"custom:test_rifleman":{"name":"Rifleman","armour":{"head":"empty","chest":"inherit"},
            "items":{"0":{"item":"minecraft:iron_sword","count":1},"40":{"item":"minecraft:shield","count":1}},
            "skins":["minecraft:textures/entity/player/wide/steve.png"]}}
            """).getAsJsonObject();
        JsonObject teams = JsonParser.parseString("""
            {"custom:test_blue":{"name":"Blue","colour":"#3366FF","classes":["custom:test_rifleman"]}}
            """).getAsJsonObject();
        EditorCatalog.installClient(new TeamEditorStore.Document(teams, classes));
        PlayerClass playerClass = PlayerClass.getPlayerClass("custom:test_rifleman");
        assertNotNull(playerClass);
        assertSame(playerClass, FlansModApi.getPlayerClass("custom:test_rifleman").orElseThrow());
        assertTrue(FlansModApi.getPlayerClasses().contains(playerClass));
        assertEquals(List.of(playerClass), Team.getTeam("custom:test_blue").getClasses());
        assertFalse(playerClass.usesTeamArmour(EquipmentSlot.HEAD));
        assertTrue(playerClass.usesTeamArmour(EquipmentSlot.CHEST));
        assertEquals(Items.SHIELD, playerClass.createStartingInventory().get(40).getItem());
        assertEquals(2, playerClass.createStartingItems().size());
        assertFalse(PlayerClass.packValues().contains(playerClass));
        EditorCatalog.clearClient();
        assertNull(PlayerClass.getPlayerClass("custom:test_rifleman"));
    }

    @Test
    void packOverridesRemainBoundToTheirPackWhenShortnamesCollide()
    {
        PlayerClass first = new ServerTestClass();
        PlayerClass second = new ServerTestClass();
        first.load(new TypeFile("first", EnumType.PLAYER_CLASS, new ContentPack("Editor test pack A", Path.of("a")), List.of("ShortName editor_test_shared", "Name First")));
        second.load(new TypeFile("second", EnumType.PLAYER_CLASS, new ContentPack("Editor test pack B", Path.of("b")), List.of("ShortName editor_test_shared", "Name Second")));
        JsonObject classes = new JsonObject();
        JsonObject patch = new JsonObject();
        patch.addProperty("name", "Edited second");
        classes.add(EditorCatalog.key(second), patch);
        EditorCatalog.installClient(new TeamEditorStore.Document(new JsonObject(), classes));
        assertEquals("First", PlayerClass.getPlayerClass(first.getShortName()).getName());
        assertEquals("Edited second", PlayerClass.getPlayerClass(second.getShortName()).getName());
        assertEquals("Second", second.getName());
        patch.addProperty("disabled", true);
        EditorCatalog.installClient(new TeamEditorStore.Document(new JsonObject(), classes));
        assertNull(PlayerClass.getPlayerClass(second.getShortName()));
        assertNotNull(PlayerClass.getPlayerClass(first.getShortName()));
        EditorCatalog.clearClient();
        assertSame(second, PlayerClass.getPlayerClass(second.getShortName()));
    }

    private static final class ServerTestClass extends PlayerClass
    {
        @Override
        protected void readClient(TypeFile file)
        {
            // This test exercises definitions without starting the client renderer.
        }
    }
}
