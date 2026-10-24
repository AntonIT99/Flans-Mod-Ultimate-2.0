package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.presets.*;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcPresetControlsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static EntityCustomNpc npc()
    {
        EntityCustomNpc npc = mock(EntityCustomNpc.class);
        npc.modelData = mock(ModelData.class);
        npc.modelData.extra = new CompoundTag();
        return npc;
    }

    @Test
    void manualSelectionSurvivesSkinLayoutChangesAndChangesOnlyWhenAnotherClassIsSelected()
    {
        EntityCustomNpc npc = npc();
        npc.modelData.extra.putString(FlanSoldierSelection.CLASS_KEY, "rifleman");
        assertEquals(NpcPreset.HUMAN, NpcPresetControls.selected(npc));
        NpcPresetControls.select(npc, NpcPreset.PLAYER.ordinal());
        npc.modelData.extra.putInt(FlanSoldierSelection.SKIN_KEY, 1);
        when(npc.modelData.getEntityName()).thenReturn(ResourceLocation.tryParse("customnpcs:customnpc64x32"));
        assertEquals(NpcPreset.PLAYER, NpcPresetControls.selected(npc));
        assertEquals(NpcPreset.PLAYER, NpcPresetControls.selected(npc));
        npc.modelData.extra.putString(FlanSoldierSelection.CLASS_KEY, "medic");
        assertEquals(NpcPreset.HUMAN, NpcPresetControls.selected(npc));
    }

    @Test
    void shipClassificationWinsOverVehicleAndPaintjobsPreserveManualSelection()
    {
        EntityCustomNpc npc = npc();
        FlanModelEntity model = mock(FlanModelEntity.class);
        IDriveableType definition = mock(IDriveableType.class);
        when(npc.modelData.getEntity(npc)).thenReturn(model);
        when(model.getInfoType()).thenReturn(definition);
        when(definition.getDriveableKind()).thenReturn(DriveableKind.VEHICLE);
        when(definition.isShip()).thenReturn(true);
        when(npc.modelData.getEntityName()).thenReturn(ResourceLocation.tryParse("wolffsmodnpcs:ship"));
        assertEquals(NpcPreset.SHIP, NpcPresetControls.selected(npc));
        NpcPresetControls.select(npc, NpcPreset.TURRET.ordinal());
        npc.modelData.extra.putInt(FlanModelEntity.PAINTJOB_KEY, 1);
        assertEquals(NpcPreset.TURRET, NpcPresetControls.selected(npc));
        when(npc.modelData.getEntityName()).thenReturn(ResourceLocation.tryParse("wolffsmodnpcs:other_ship"));
        assertEquals(NpcPreset.SHIP, NpcPresetControls.selected(npc));
    }

    @Test
    void aircraftPresetUsesHelicopterFlightAndCompatibleTacticsForRotorcraft()
    {
        EntityCustomNpc npc = npc();
        FlanModelEntity model = mock(FlanModelEntity.class);
        IDriveableType definition = mock(IDriveableType.class);
        when(npc.modelData.getEntity(npc)).thenReturn(model);
        when(model.getInfoType()).thenReturn(definition);
        when(definition.getDriveableKind()).thenReturn(DriveableKind.PLANE);
        when(definition.isHelicopter()).thenReturn(true);
        assertEquals(NpcPreset.PLANE, NpcPresetControls.selected(npc));
        assertEquals(com.wolffsmod.npcs.sim.NpcTuning.Flight.HELICOPTER.ordinal(), NpcPreset.PLANE.value(npc, new NpcPreset.Target(NpcPresetSetting.FLIGHT_TYPE, 1)));
        assertEquals(com.wolffsmod.npcs.sim.NpcTuning.Tactic.STRAFE.ordinal(), NpcPreset.PLANE.value(npc, new NpcPreset.Target(NpcPresetSetting.TACTIC, 0)));
    }
}
