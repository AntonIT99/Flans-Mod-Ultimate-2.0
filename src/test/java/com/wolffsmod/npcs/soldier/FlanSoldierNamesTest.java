package com.wolffsmod.npcs.soldier;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.IPlayerClass;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.names.SoldierNameAssociations;
import com.wolffsmod.npcs.names.SoldierNames;
import com.wolffsmod.npcs.network.SoldierNamePacket;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("IgnoreResultOfCall") // Mockito's static stubbing callbacks intentionally discard the return value.
class FlanSoldierNamesTest
{
    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored") // Mockito's static stubbing callback intentionally discards the result.
    void explicitPickerGenderOverridesAssociationAndInvalidRequestsAreIgnored()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.display = mock(DataDisplay.class);
        IPlayerClass playerClass = mock(IPlayerClass.class);
        when(playerClass.getShortName()).thenReturn("class");
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of(), Map.of("class", "wolffsmodnpcs:french"));
        try (var config = mockStatic(SoldierNameAssociations.class); var names = mockStatic(SoldierNames.class, CALLS_REAL_METHODS))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            names.when(() -> SoldierNames.generate(70, 2)).thenReturn("Claire Martin");
            names.when(() -> SoldierNames.generate(70, 1)).thenReturn("Louis Martin");
            FlanSoldierSelection selection = new FlanSoldierSelection(null, playerClass);
            assertEquals("Claire Martin", FlanSoldierNames.apply(npc, selection, 2).orElseThrow().name());
            verify(npc.display).setMarkovGender(2);
            assertEquals("Louis Martin", FlanSoldierNames.apply(npc, selection, 1).orElseThrow().name());
            verify(npc.display).setMarkovGender(1);
            assertTrue(FlanSoldierNames.request(null, 42, "", "class", 0).isEmpty());
            assertTrue(FlanSoldierNames.request(null, 42, "", "class", 3).isEmpty());
        }
    }

    @Test
    void romanTeamAppliesBuiltInGeneratorAndReturnsItsAssociationId()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.display = mock(DataDisplay.class);
        ITeam team = mock(ITeam.class);
        IPlayerClass playerClass = mock(IPlayerClass.class);
        when(team.getShortName()).thenReturn(com.flansmodultimate.util.ResourceUtils.sanitize("WolffRome_Empire"));
        when(playerClass.getShortName()).thenReturn(com.flansmodultimate.util.ResourceUtils.sanitize("WolffRome_LegionaryHamata"));
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("WolffRome_Empire", SoldierNames.ROMAN_ID), Map.of());
        try (var config = mockStatic(SoldierNameAssociations.class); var generator = mockStatic(nikedemos.markovnames.generators.MarkovGenerator.class))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            generator.when(() -> nikedemos.markovnames.generators.MarkovGenerator.fetch(0, 1)).thenReturn("Marcus");
            var reply = FlanSoldierNames.apply(npc, new FlanSoldierSelection(team, playerClass)).orElseThrow();
            verify(npc.display).setMarkovGeneratorId(0);
            verify(npc.display).setMarkovGender(1);
            verify(npc.display).setName("Marcus");
            assertEquals(SoldierNames.ROMAN_ID, reply.nameList());
            assertEquals("Marcus", reply.name());
        }
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void refusesRequestsWithoutTheSameNpcOpenInTheEditor()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.display = mock(DataDisplay.class);
        try (var editor = mockStatic(NoppesUtilServer.class); var config = mockStatic(SoldierNameAssociations.class))
        {
            assertTrue(FlanSoldierNames.request(player, 42, "team", "class").isEmpty());
            FlanSoldierNames.confirm(player, new SoldierNamePacket(42, "team", "class", true, "wolffsmodnpcs:german", "Injected name"));
            editor.when(() -> NoppesUtilServer.getEditingNpc(player)).thenReturn(npc);
            when(npc.getId()).thenReturn(43);
            assertTrue(FlanSoldierNames.request(player, 42, "team", "class").isEmpty());
            config.verifyNoInteractions();
            verifyNoInteractions(npc.display);
        }
    }

    @Test
    void appliesClassOverrideUsingConfiguredGenderAndReturnsSameNameToEditor()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.display = mock(DataDisplay.class);
        when(npc.getId()).thenReturn(42);
        when(npc.display.getMarkovGender()).thenReturn(2);
        ITeam team = mock(ITeam.class);
        IPlayerClass playerClass = mock(IPlayerClass.class);
        when(team.getShortName()).thenReturn("team");
        when(playerClass.getShortName()).thenReturn("class");
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of("team", "wolffsmodnpcs:german"), Map.of("class", "wolffsmodnpcs:russian"), Map.of(), Map.of("class", "female"));
        try (var config = mockStatic(SoldierNameAssociations.class); var names = mockStatic(SoldierNames.class, CALLS_REAL_METHODS))
        {
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            names.when(() -> SoldierNames.generate(13, 2)).thenReturn("Anna Ivanova");
            var reply = FlanSoldierNames.apply(npc, new FlanSoldierSelection(team, playerClass)).orElseThrow();
            verify(npc.display).setMarkovGeneratorId(13);
            verify(npc.display).setMarkovGender(2);
            assertEquals(2, reply.gender());
            verify(npc.display).setName("Anna Ivanova");
            assertEquals("Anna Ivanova", reply.name());
            assertEquals("wolffsmodnpcs:russian", reply.nameList());
            assertEquals(42, reply.entityId());
            assertTrue(reply.state());
            assertTrue(npc.updateClient);
        }
    }

    @Test
    void unmappedOrExplicitlyDisabledClassLeavesNameAndGeneratorAlone()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.display = mock(DataDisplay.class);
        IPlayerClass playerClass = mock(IPlayerClass.class);
        when(playerClass.getShortName()).thenReturn("class");
        SoldierNameAssociations empty = new SoldierNameAssociations(Map.of(), Map.of());
        SoldierNameAssociations disabled = new SoldierNameAssociations(Map.of("team", "wolffsmodnpcs:german"), Map.of("class", ""));
        try (var config = mockStatic(SoldierNameAssociations.class))
        {
            config.when(SoldierNameAssociations::current).thenReturn(empty);
            assertTrue(FlanSoldierNames.apply(npc, new FlanSoldierSelection(null, playerClass)).isEmpty());
            ITeam team = mock(ITeam.class);
            when(team.getShortName()).thenReturn("team");
            config.when(SoldierNameAssociations::current).thenReturn(disabled);
            assertTrue(FlanSoldierNames.apply(npc, new FlanSoldierSelection(team, playerClass)).isEmpty());
            verifyNoInteractions(npc.display);
            assertFalse(npc.updateClient);
        }
    }

    @Test
    void acknowledgementRestoresOnlyTheIssuedNameAfterAnOlderEditorSave()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.display = mock(DataDisplay.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(npc.level()).thenReturn(level);
        when(npc.getId()).thenReturn(42);
        when(npc.isAlive()).thenReturn(true);
        when(level.getGameTime()).thenReturn(20L);
        Inventory inventory = mock(Inventory.class);
        ItemStack wand = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getSelected()).thenReturn(wand);
        when(wand.getItem()).thenReturn(CustomItems.wand);
        IPlayerClass playerClass = mock(IPlayerClass.class);
        when(playerClass.getShortName()).thenReturn("class");
        SoldierNameAssociations associations = new SoldierNameAssociations(Map.of(), Map.of("class", "wolffsmodnpcs:german"));
        try (var editor = mockStatic(NoppesUtilServer.class);
            var permissions = mockStatic(CustomNpcsPermissions.class);
            var api = mockStatic(FlansModApi.class);
            var config = mockStatic(SoldierNameAssociations.class))
        {
            editor.when(() -> NoppesUtilServer.getEditingNpc(player)).thenReturn(npc);
            permissions.when(() -> CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_DISPLAY)).thenReturn(true);
            api.when(() -> FlansModApi.getPlayerClass("class")).thenReturn(Optional.of(playerClass));
            config.when(SoldierNameAssociations::current).thenReturn(associations);
            SoldierNamePacket reply = FlanSoldierNames.request(player, 42, "", "class", 1).orElseThrow();
            assertEquals(1, reply.gender());
            npc.display.setName("Old saved name");
            npc.display.setMarkovGender(2);
            clearInvocations(npc.display);
            FlanSoldierNames.confirm(player, new SoldierNamePacket(42, "", "class", true, reply.nameList(), "Injected name", reply.gender()));
            verifyNoInteractions(npc.display);
            FlanSoldierNames.confirm(player, new SoldierNamePacket(42, "", "class", true, reply.nameList(), reply.name(), 2));
            verifyNoInteractions(npc.display);
            FlanSoldierNames.confirm(player, reply);
            verify(npc.display).setName(reply.name());
            verify(npc.display).setMarkovGeneratorId(10);
            verify(npc.display).setMarkovGender(1);
            clearInvocations(npc.display);
            FlanSoldierNames.confirm(player, reply);
            verifyNoInteractions(npc.display);
        }
    }
}
