package com.wolffsmod.npcs.soldier;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SoldierCloneNamesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void regeneratesOnlyNewIdentitiesWithTheSavedClassAndGender()
    {
        EntityCustomNpc npc = mock(EntityCustomNpc.class);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        UUID original = UUID.randomUUID();
        when(npc.getUUID()).thenReturn(original);
        npc.modelData = mock(ModelData.class);
        npc.modelData.extra = new CompoundTag();
        CompoundTag saved = new CompoundTag();
        SoldierCloneNames.save(npc, saved);
        npc.modelData.extra.putBoolean(SoldierCloneNames.REGENERATE, true);
        npc.modelData.extra.putInt(SoldierCloneNames.GENDER, 2);
        IPlayerClass playerClass = mock(IPlayerClass.class);
        when(playerClass.getShortName()).thenReturn("rifleman");
        FlanSoldierSelection selection = new FlanSoldierSelection(null, playerClass);
        selection.write(npc.modelData.extra);
        try (var selections = mockStatic(FlansModApi.class); var naming = mockStatic(FlanSoldierNames.class))
        {
            selections.when(() -> FlansModApi.getPlayerClass("rifleman")).thenReturn(Optional.of(playerClass));
            SoldierCloneNames.loaded(npc, saved);
            naming.verifyNoInteractions();
            when(npc.getUUID()).thenReturn(UUID.randomUUID());
            SoldierCloneNames.loaded(npc, saved);
            naming.verify(() -> FlanSoldierNames.apply(npc, selection, 2));
            naming.clearInvocations();
            // Saving and reloading the clone must preserve its newly generated name.
            SoldierCloneNames.save(npc, saved);
            SoldierCloneNames.loaded(npc, saved);
            naming.verifyNoInteractions();
            when(npc.getUUID()).thenReturn(UUID.randomUUID());
            npc.modelData.extra.putBoolean(SoldierCloneNames.REGENERATE, false);
            SoldierCloneNames.loaded(npc, saved);
            naming.verifyNoInteractions();
            npc.modelData.extra.putBoolean(SoldierCloneNames.REGENERATE, true);
            SoldierCloneNames.loaded(npc, new CompoundTag());
            naming.verifyNoInteractions();
            selections.when(() -> FlansModApi.getPlayerClass("rifleman")).thenReturn(Optional.empty());
            SoldierCloneNames.loaded(npc, saved);
            naming.verifyNoInteractions();
        }
    }

    @Test
    void missingOrInvalidGenderDefaultsToMale()
    {
        CompoundTag extra = new CompoundTag();
        assertEquals(1, SoldierCloneNames.selectedGender(extra));
        extra.putInt(SoldierCloneNames.GENDER, -1);
        assertEquals(1, SoldierCloneNames.selectedGender(extra));
        extra.putInt(SoldierCloneNames.GENDER, 2);
        assertEquals(2, SoldierCloneNames.selectedGender(extra));
    }
}
