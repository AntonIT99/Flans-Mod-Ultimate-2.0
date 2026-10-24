package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.IDriveableType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.client.CombatPages.Group;
import com.wolffsmod.npcs.client.ModelTuningRows.Row;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.DriveType;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import noppes.npcs.shared.client.gui.components.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModelTuningRowsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void everyTuningValueAModelCanSupplyHasARowOnTheModelPropertiesPage()
    {
        EntityNPCInterface npc = npc(new NpcTuning());
        try (var models = mockStatic(NpcModels.class); var kinds = mockStatic(FlanModelKind.class); var mobility = mockStatic(NpcMobility.class))
        {
            model(npc, models, kinds, FlanModelKind.VEHICLE);
            mobility.when(() -> NpcMobility.modelSpeed(npc)).thenReturn(OptionalDouble.of(0.5D));
            Set<Value> rows = Arrays.stream(Row.values()).map(ModelTuningRowsTest::value).filter(Objects::nonNull).collect(Collectors.toSet());
            for (Value value : Value.values())
            {
                if (GroundHandlingControls.supported(npc, value))
                    assertTrue(rows.contains(value), value + " inherits from the model but has no row");
            }
            assertTrue(rows.contains(Value.DRIVE_TYPE));
        }
    }

    @Test
    void aGroundVehicleOffersBothMovementTabsAndOtherModelsOnlyTheirGunsAimSpeeds()
    {
        EntityNPCInterface npc = npc(new NpcTuning());
        try (var models = mockStatic(NpcModels.class); var kinds = mockStatic(FlanModelKind.class))
        {
            model(npc, models, kinds, FlanModelKind.VEHICLE);
            assertEquals(List.of(Row.LAND_SPEED, Row.REVERSE_SPEED, Row.ACCELERATION, Row.BRAKING, Row.COASTING, Row.MASS, Row.ENGINE_POWER), ModelTuningRows.rows(npc, Group.DRIVING));
            assertEquals(List.of(Row.DRIVE_TYPE, Row.HULL_TURN_RATE, Row.TURN_RADIUS, Row.TRAVERSE, Row.ELEVATION), ModelTuningRows.rows(npc, Group.TURNING));
            model(npc, models, kinds, FlanModelKind.AA_GUN);
            assertFalse(ModelTuningRows.visible(npc, Group.DRIVING));
            assertEquals(List.of(Row.TRAVERSE, Row.ELEVATION), ModelTuningRows.rows(npc, Group.TURNING));
            models.when(() -> NpcModels.flan(npc)).thenReturn(null);
            assertFalse(ModelTuningRows.visible(npc, Group.DRIVING));
            assertFalse(ModelTuningRows.visible(npc, Group.TURNING));
            assertTrue(ModelTuningRows.visible(npc, Group.GENERAL));
        }
    }

    @Test
    void customCopiesTheModelsValueAndOnlyDrivingValuesTurnThePresetToCustom()
    {
        NpcTuning tuning = new NpcTuning();
        tuning.applyPreset(NpcTuning.DrivePreset.CAR);
        tuning.set(Value.DRIVE_TYPE, DriveType.AUTO.ordinal());
        EntityNPCInterface npc = npc(tuning);
        ModelTuningRows rows = new ModelTuningRows(mock(GuiBasic.class), npc);
        try (var models = mockStatic(NpcModels.class); var kinds = mockStatic(FlanModelKind.class); var state = mockStatic(NpcVehicleState.class))
        {
            model(npc, models, kinds, FlanModelKind.VEHICLE);
            state.when(() -> NpcVehicleState.modelAimStep(npc, false)).thenReturn(2.5F);
            assertTrue(rows.button(button(ModelTuningRows.MODE_ID + Row.TRAVERSE.ordinal(), 1)));
            assertEquals(50, tuning.get(Value.AIM_SPEED_YAW));
            assertEquals(NpcTuning.DrivePreset.CAR.ordinal(), tuning.get(Value.DRIVE_PRESET));
            // A tank's automatic type comes to tracked, which Custom starts from.
            assertTrue(rows.button(button(ModelTuningRows.MODE_ID + Row.DRIVE_TYPE.ordinal(), 1)));
            assertEquals(DriveType.TRACKED, tuning.driveType());
            assertEquals(NpcTuning.DrivePreset.CUSTOM.ordinal(), tuning.get(Value.DRIVE_PRESET));
            assertTrue(rows.button(button(ModelTuningRows.VALUE_ID + Row.DRIVE_TYPE.ordinal(), 2)));
            assertEquals(DriveType.HEAVY_TRACKED, tuning.driveType());
            rows.inheritAll(Group.TURNING);
            assertEquals(DriveType.AUTO, tuning.driveType());
            assertEquals(0, tuning.get(Value.AIM_SPEED_YAW));
        }
    }

    @Test
    void anOwnValueIsKeptWithinItsLimitsAndAnInheritedOneIsNotOverwritten()
    {
        NpcTuning tuning = new NpcTuning();
        EntityNPCInterface npc = npc(tuning);
        ModelTuningRows rows = new ModelTuningRows(mock(GuiBasic.class), npc);
        try (var models = mockStatic(NpcModels.class); var kinds = mockStatic(FlanModelKind.class))
        {
            model(npc, models, kinds, FlanModelKind.VEHICLE);
            GuiTextFieldNop field = field(ModelTuningRows.VALUE_ID + Row.HULL_TURN_RATE.ordinal(), 90);
            assertFalse(rows.field(field));
            assertEquals(0, tuning.get(Value.HULL_TURN_RATE));
            tuning.set(Value.HULL_TURN_RATE, 40);
            assertTrue(rows.field(field(ModelTuningRows.VALUE_ID + Row.HULL_TURN_RATE.ordinal(), 99999)));
            assertEquals(Value.HULL_TURN_RATE.maximum(), tuning.get(Value.HULL_TURN_RATE));
        }
    }

    private static Value value(Row row)
    {
        return switch (row)
        {
            case LAND_SPEED -> Value.LAND_SPEED;
            case REVERSE_SPEED -> Value.DRIVE_REVERSE_SPEED;
            case ACCELERATION -> Value.DRIVE_ACCELERATION;
            case BRAKING -> Value.DRIVE_BRAKING;
            case COASTING -> Value.DRIVE_COASTING;
            case DRIVE_TYPE -> Value.DRIVE_TYPE;
            case HULL_TURN_RATE -> Value.HULL_TURN_RATE;
            case TURN_RADIUS -> Value.TURN_RADIUS;
            case TRAVERSE -> Value.AIM_SPEED_YAW;
            case ELEVATION -> Value.AIM_SPEED_PITCH;
            case MASS, ENGINE_POWER -> null;
        };
    }

    private static void model(EntityNPCInterface npc, org.mockito.MockedStatic<NpcModels> models, org.mockito.MockedStatic<FlanModelKind> kinds, FlanModelKind kind)
    {
        FlanModelEntity model = mock(FlanModelEntity.class);
        IDriveableType type = mock(IDriveableType.class);
        when(type.isTracked()).thenReturn(true);
        when(model.getInfoType()).thenReturn(type);
        models.when(() -> NpcModels.flan(npc)).thenReturn(model);
        kinds.when(() -> FlanModelKind.of(type)).thenReturn(kind);
    }

    private static GuiButtonNop button(int id, int value)
    {
        GuiButtonNop button = mock(GuiButtonNop.class);
        button.id = id;
        when(button.getValue()).thenReturn(value);
        return button;
    }

    private static GuiTextFieldNop field(int id, int value)
    {
        GuiTextFieldNop field = mock(GuiTextFieldNop.class);
        field.id = id;
        when(field.isInteger()).thenReturn(true);
        when(field.getInteger()).thenReturn(value);
        return field;
    }

    private static EntityNPCInterface npc(NpcTuning tuning)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning()).thenReturn(tuning);
        return npc;
    }
}
