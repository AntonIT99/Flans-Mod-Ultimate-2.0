package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.driveables.GroundPropulsion;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcDriving;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import noppes.npcs.shared.client.gui.components.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroundHandlingControlsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void inheritedReadoutKeepsItsFractionAndCannotOverwriteTheStoredSentinel()
    {
        NpcTuning tuning = new NpcTuning();
        EntityNPCInterface npc = npc(tuning);
        GuiBasic gui = mock(GuiBasic.class);
        List<Object> arguments = new ArrayList<>();
        try (var driving = mockStatic(NpcDriving.class);
            var fields = mockConstruction(GuiTextFieldNop.class, (field, context) -> arguments.addAll(context.arguments()));
            var buttons = mockConstruction(GuiButtonNop.class))
        {
            driving.when(() -> NpcDriving.settings(npc)).thenReturn(settings());
            assertTrue(GroundHandlingControls.render(gui, npc, Value.HULL_TURN_RATE, 1000, 0, 0, true));
            assertEquals("30.5", arguments.get(arguments.size() - 1));
            verify(fields.constructed().get(0)).setEditable(false);
            assertEquals(1, buttons.constructed().size());
            assertEquals(0, tuning.get(Value.HULL_TURN_RATE));
            assertTrue(GroundHandlingControls.inherited(npc, Value.HULL_TURN_RATE));
        }
    }

    @Test
    void selectingCustomCopiesTheEffectiveValueAndInheritRestoresTheSentinel()
    {
        NpcTuning tuning = new NpcTuning();
        EntityNPCInterface npc = npc(tuning);
        GuiButtonNop button = mock(GuiButtonNop.class);
        button.id = 1900 + Value.HULL_TURN_RATE.ordinal();
        try (var driving = mockStatic(NpcDriving.class))
        {
            driving.when(() -> NpcDriving.settings(npc)).thenReturn(settings());
            when(button.getValue()).thenReturn(1);
            assertTrue(GroundHandlingControls.button(npc, button));
            assertEquals(31, tuning.get(Value.HULL_TURN_RATE));
            assertFalse(GroundHandlingControls.inherited(npc, Value.HULL_TURN_RATE));
            when(button.getValue()).thenReturn(0);
            assertTrue(GroundHandlingControls.button(npc, button));
            assertEquals(0, tuning.get(Value.HULL_TURN_RATE));
            assertFalse(GroundHandlingControls.render(mock(GuiBasic.class), npc, Value.FOLLOW_DISTANCE, 1001, 0, 0, true));
        }
    }

    @Test
    void massAndEnginePowerReadTheModelsRealisticProfileAndIgnoreALegacyOne()
    {
        NpcTuning tuning = new NpcTuning();
        EntityNPCInterface npc = npc(tuning);
        try (var driving = mockStatic(NpcDriving.class))
        {
            GroundPropulsion realistic = mock(GroundPropulsion.class);
            when(realistic.realistic()).thenReturn(true);
            when(realistic.massKg()).thenReturn(OptionalDouble.of(40000D));
            when(realistic.enginePowerKw()).thenReturn(OptionalDouble.of(300D));
            driving.when(() -> NpcDriving.propulsion(eq(npc), any())).thenReturn(Optional.of(realistic));
            assertTrue(GroundHandlingControls.supported(npc, Value.MASS));
            assertTrue(GroundHandlingControls.supported(npc, Value.ENGINE_POWER));
            assertEquals(40000D, GroundHandlingControls.effective(npc, Value.MASS));
            assertEquals(300D, GroundHandlingControls.effective(npc, Value.ENGINE_POWER));

            GroundPropulsion legacy = mock(GroundPropulsion.class);
            when(legacy.realistic()).thenReturn(false);
            driving.when(() -> NpcDriving.propulsion(eq(npc), any())).thenReturn(Optional.of(legacy));
            assertFalse(GroundHandlingControls.supported(npc, Value.MASS));
            assertFalse(GroundHandlingControls.supported(npc, Value.ENGINE_POWER));
        }
    }

    private static NpcDriving.Settings settings()
    {
        return new NpcDriving.Settings(false, 30.5F / 20F, 12F, 0.05F, 0.125F, 0.03F, 0.1875F, 0.45F, 0.15F, true, 110F, 10D, false);
    }

    private static EntityNPCInterface npc(NpcTuning tuning)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning()).thenReturn(tuning);
        return npc;
    }
}
