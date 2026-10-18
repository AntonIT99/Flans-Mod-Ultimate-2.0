package com.wolffsmod.npcs.soldier;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.client.FlanSoldierBrowser;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.nbt.CompoundTag;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlanSoldierSkinsTest
{
    @Test
    void multiSkinClassesOpenAVariantFolderAndBackReturnsToTheClasses()
    {
        IPlayerClass playerClass = mock(IPlayerClass.class);
        ITeam team = mock(ITeam.class);
        IContentPack pack = mock(IContentPack.class);
        when(pack.getName()).thenReturn("Test pack");
        when(pack.getPath()).thenReturn(Path.of("test-pack"));
        when(team.getContentPack()).thenReturn(pack);
        when(team.getName()).thenReturn("Team");
        doReturn(List.of(playerClass)).when(team).getClasses();
        when(playerClass.getName()).thenReturn("Rifleman");
        when(playerClass.getSkinOverrideVariants()).thenReturn(List.of(new SkinOverrideVariant("first", null), new SkinOverrideVariant("second", null)));
        try (MockedStatic<FlansModApi> api = mockStatic(FlansModApi.class))
        {
            api.when(FlansModApi::getTeams).thenReturn(List.of(team));
            FlanSoldierBrowser browser = new FlanSoldierBrowser();
            assertEquals(List.of("Test pack"), browser.labels());
            assertTrue(browser.open(0).isEmpty());
            assertEquals(List.of("Team"), browser.labels());
            assertTrue(browser.open(0).isEmpty());
            assertEquals(List.of("Rifleman"), browser.labels());
            assertEquals(0, browser.open(0).orElseThrow().skin());
            assertTrue(browser.browsingSkins());
            assertEquals(List.of("first", "second"), browser.labels());
            FlanSoldierSelection selected = browser.open(1).orElseThrow();
            assertEquals(1, selected.skin());
            assertEquals(1, browser.selectedRow(selected));
            assertTrue(browser.back());
            assertFalse(browser.browsingSkins());
            assertEquals(List.of("Rifleman"), browser.labels());
            assertEquals(0, browser.selectedRow(selected));
            when(playerClass.getSkinOverrideVariants()).thenReturn(List.of(new SkinOverrideVariant("only", null)));
            assertEquals(0, browser.open(0).orElseThrow().skin());
            assertFalse(browser.browsingSkins());
        }
    }

    @Test
    void selectedSkinSurvivesSavingWhileOldAndInvalidSelectionsUseTheFirstSkin()
    {
        IPlayerClass playerClass = mock(IPlayerClass.class);
        ITeam team = mock(ITeam.class);
        when(team.getShortName()).thenReturn("team");
        when(playerClass.getShortName()).thenReturn("class");
        doReturn(List.of(playerClass)).when(team).getClasses();
        when(playerClass.getSkinOverrideVariants()).thenReturn(List.of(new SkinOverrideVariant("first", null), new SkinOverrideVariant("second", null)));
        try (MockedStatic<FlansModApi> api = mockStatic(FlansModApi.class))
        {
            api.when(() -> FlansModApi.getTeam("team")).thenReturn(Optional.of(team));
            CompoundTag extra = new CompoundTag();
            FlanSoldierSelection selected = new FlanSoldierSelection(team, playerClass, 1);
            selected.write(extra);
            assertEquals(selected, FlanSoldierSelection.read(extra).orElseThrow());
            extra.remove(FlanSoldierSelection.SKIN_KEY);
            assertEquals(0, FlanSoldierSelection.read(extra).orElseThrow().skin());
            extra.putInt(FlanSoldierSelection.SKIN_KEY, 999);
            assertEquals(0, FlanSoldierSelection.read(extra).orElseThrow().skin());
            extra.putInt(FlanSoldierSelection.SKIN_KEY, -1);
            assertEquals(0, FlanSoldierSelection.read(extra).orElseThrow().skin());
        }
    }
}
