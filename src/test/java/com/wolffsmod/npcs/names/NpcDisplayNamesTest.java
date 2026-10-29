package com.wolffsmod.npcs.names;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.entity.data.DataDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcDisplayNamesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void explicitClassActionsKeepCanonicalFieldsAndPersonalNamingClearsOnlyTheNameBinding()
    {
        NpcDisplayNames state = new NpcDisplayNames();
        DataDisplay display = mock(DataDisplay.class, withSettings().extraInterfaces(NpcDisplayNames.Holder.class));
        when(((NpcDisplayNames.Holder) display).wolffsmodnpcsDisplayNames()).thenReturn(state);
        IPlayerClass type = mock(IPlayerClass.class);
        when(type.getShortName()).thenReturn("crew");
        when(type.getName()).thenReturn("Crewman");
        NpcDisplayNames.bindName(display, type);
        NpcDisplayNames.bindTitle(display, type);
        verify(display).setName("Crewman");
        verify(display).setTitle("Crewman");
        assertEquals(new ContentNameBinding(false, "crew", "Crewman"), state.getName());
        assertEquals(state.getName(), state.getTitle());
        NpcDisplayNames.literalName(display, "Crewman");
        assertNull(state.getName());
        assertNotNull(state.getTitle());
        verify((NpcDisplayNames.Holder) display, times(3)).wolffsmodnpcsNamesChanged();
    }

    @Test
    void nameAndTitleRoundTripIndependentlyAndMissingMetadataNeverGuesses()
    {
        NpcDisplayNames state = new NpcDisplayNames();
        ContentNameBinding name = new ContentNameBinding(false, "crew", "Crewman");
        ContentNameBinding title = new ContentNameBinding(true, "army", "Army");
        state.setName(name);
        state.setTitle(title);
        CompoundTag tag = new CompoundTag();
        state.save(tag, "Crewman", "Army");
        NpcDisplayNames loaded = new NpcDisplayNames();
        loaded.load(tag, "Crewman", "Army");
        assertEquals(name, loaded.getName());
        assertEquals(title, loaded.getTitle());
        loaded.load(tag, "Alice", "Army");
        assertNull(loaded.getName());
        assertEquals(title, loaded.getTitle());
        loaded.load(new CompoundTag(), "Crewman", "Army");
        assertNull(loaded.getName());
        assertNull(loaded.getTitle());
    }

    @Test
    void manualFactionRenameAndMissingContentFallBackToLiteralNames()
    {
        ContentNameBinding binding = new ContentNameBinding(true, "army", "Army");
        assertEquals("Builder's Faction", binding.component("Builder's Faction").getString());
        assertTrue(ContentNameBinding.read(binding.save(), "Builder's Faction").isEmpty());
        try (var api = mockStatic(FlansModApi.class))
        {
            api.when(() -> FlansModApi.getTeam("army")).thenReturn(Optional.empty());
            assertEquals("Army", binding.component("Army").getString());
        }
    }

    @Test
    void teamAndClassReferencesUseIdsAndPreserveComponentsForTheViewingClient()
    {
        ITeam team = mock(ITeam.class);
        when(team.getShortName()).thenReturn("army_2");
        when(team.getName()).thenReturn("Army");
        when(team.getNameComponent()).thenReturn(Component.translatableWithFallback("team.pack.army", "Army"));
        IPlayerClass type = mock(IPlayerClass.class);
        when(type.getNameComponent()).thenReturn(Component.translatableWithFallback("class.pack.crew", "Crewman"));
        try (var api = mockStatic(FlansModApi.class))
        {
            api.when(() -> FlansModApi.getTeam("army_2")).thenReturn(Optional.of(team));
            api.when(() -> FlansModApi.getPlayerClass("crew")).thenReturn(Optional.of(type));
            var binding = ContentNameBinding.of(team);
            assertEquals("army_2", binding.id());
            assertEquals("team.pack.army", ((TranslatableContents) binding.component("Army").getContents()).getKey());
            var classBinding = new ContentNameBinding(false, "crew", "Crewman");
            assertEquals("class.pack.crew", ((TranslatableContents) classBinding.component("Crewman").getContents()).getKey());
        }
    }

    @Test
    void savingAnEditedLiteralRemovesStaleBindingMetadata()
    {
        NpcDisplayNames state = new NpcDisplayNames();
        state.setName(new ContentNameBinding(false, "crew", "Crewman"));
        CompoundTag tag = new CompoundTag();
        state.save(tag, "Crewman", "");
        state.save(tag, "Alice", "");
        NpcDisplayNames loaded = new NpcDisplayNames();
        loaded.load(tag, "Alice", "");
        assertNull(loaded.getName());
        assertTrue(tag.isEmpty());
    }
}
