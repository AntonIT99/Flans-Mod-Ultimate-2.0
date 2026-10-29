package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.GroundHandling;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcDriving.*;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcHandlingInheritanceTest
{
    private static final GroundHandling MODEL = new GroundHandling(OptionalDouble.of(30.5D), OptionalDouble.of(12D), OptionalDouble.of(0.1875D));

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void modelPropertySwitchControlsRuntimeInheritance()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        FlanModelEntity model = mock(FlanModelEntity.class);
        IDriveableType type = mock(IDriveableType.class);
        when(model.getInfoType()).thenReturn(type);
        NpcWeaponOptions options = new NpcWeaponOptions();
        try (var models = mockStatic(NpcModels.class); var types = mockStatic(FlansEntityTypes.class))
        {
            models.when(() -> NpcModels.flan(npc)).thenReturn(model);
            types.when(() -> FlansEntityTypes.getGroundHandling(type)).thenReturn(MODEL);
            assertSame(MODEL, NpcDriving.handling(npc, options));
            options.set(Feature.TYPE_PROPERTIES, false);
            assertSame(GroundHandling.EMPTY, NpcDriving.handling(npc, options));
            options.set(Feature.TYPE_PROPERTIES, true);
            models.when(() -> NpcModels.flan(npc)).thenReturn(null);
            assertSame(GroundHandling.EMPTY, NpcDriving.handling(npc, options));
        }
    }

    @Test
    void inheritedFractionsReachTheControllerWithoutEditorRounding()
    {
        Settings settings = Settings.of(new NpcTuning(), new NpcWeaponOptions(), false, MODEL);
        assertEquals(30.5F / 20F, settings.turnStep());
        assertEquals(12F, settings.radius());
        assertFalse(settings.widenRadius());
        assertEquals(0.1875F, settings.reverseShare());
        NpcDriving driving = new NpcDriving();
        Situation turn = new Situation(0F, 90F, 100D, 0F, 100D, 4D, 0.2D, 1F, 0D, 0D);
        assertEquals(Math.toDegrees(0.2D / 12D), driving.drive(turn, settings).hullYaw(), 1.0E-6D);
        for (int tick = 0; tick < 20; tick++)
            assertEquals(Math.toDegrees(0.2D / 12D), driving.drive(turn, settings).hullYaw(), 1.0E-6D);
    }

    @Test
    void positiveCustomValuesAndSavedOverridesRemainAuthoritative()
    {
        NpcTuning tuning = new NpcTuning();
        tuning.set(Value.HULL_TURN_RATE, 80);
        tuning.set(Value.TURN_RADIUS, 70);
        tuning.set(Value.DRIVE_REVERSE_SPEED, 55);
        CompoundTag saved = new CompoundTag();
        tuning.save(saved);
        NpcTuning restored = new NpcTuning();
        restored.load(saved);
        Settings settings = Settings.of(restored, new NpcWeaponOptions(), false, MODEL);
        assertEquals(4F, settings.turnStep());
        assertEquals(7F, settings.radius());
        assertTrue(settings.widenRadius());
        assertEquals(0.55F, settings.reverseShare());
    }

    @Test
    void unspecifiedDefinitionsKeepSimulationFallbacksAndCannotReverseWhenProhibited()
    {
        Settings fallback = Settings.of(new NpcTuning(), new NpcWeaponOptions(), false, GroundHandling.EMPTY);
        assertEquals(4.5F, fallback.turnStep());
        assertEquals(3F, fallback.radius());
        assertEquals(0.4F, fallback.reverseShare());
        GroundHandling blocked = new GroundHandling(OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.of(0D));
        Settings cannotReverse = Settings.of(new NpcTuning(), new NpcWeaponOptions(), true, blocked);
        assertEquals(0F, cannotReverse.reverseShare());
        assertFalse(cannotReverse.reverse());
    }
}
