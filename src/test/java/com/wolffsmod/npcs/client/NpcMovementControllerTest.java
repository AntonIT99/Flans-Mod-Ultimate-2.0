package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcMovementControllerTest
{
    @ParameterizedTest
    @CsvSource({"false, false, FREE", "false, true, FREE", "true, false, HULL", "true, true, VEHICLE"})
    void openingOldSettingsSelectsTheirEffectiveControllerWithoutRewritingThem(boolean hull, boolean driving, NpcMovementController expected)
    {
        CompoundTag legacy = new CompoundTag();
        legacy.putBoolean(Feature.HULL_FACES_TRAVEL.name(), hull);
        legacy.putBoolean(Feature.DRIVING.name(), driving);
        CompoundTag root = new CompoundTag();
        root.put(NpcWeaponOptions.NBT_KEY, legacy);
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.load(root);

        assertEquals(expected, NpcMovementController.of(options));
        assertEquals(hull, options.enabled(Feature.HULL_FACES_TRAVEL));
        assertEquals(driving, options.enabled(Feature.DRIVING));
    }

    @ParameterizedTest
    @EnumSource(NpcMovementController.class)
    void eachChoiceRoundTripsThroughTheExistingSaveFlags(NpcMovementController controller)
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.set(Feature.MISALIGNED_THROTTLE, false);
        options.set(Feature.DRIVE_REVERSE, false);
        controller.apply(options);
        CompoundTag root = new CompoundTag();
        options.save(root);
        NpcWeaponOptions restored = new NpcWeaponOptions();
        restored.load(root);

        assertEquals(controller, NpcMovementController.of(restored));
        assertFalse(restored.enabled(Feature.MISALIGNED_THROTTLE));
        assertFalse(restored.enabled(Feature.DRIVE_REVERSE));
        assertEquals(controller != NpcMovementController.FREE, restored.enabled(Feature.HULL_FACES_TRAVEL));
        if (controller != NpcMovementController.FREE)
            assertEquals(controller == NpcMovementController.VEHICLE, restored.enabled(Feature.DRIVING));
    }

    @Test
    void switchingOffHullSteeringKeepsTheInactiveDrivingPreference()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        NpcMovementController.HULL.apply(options);
        NpcMovementController.FREE.apply(options);
        assertFalse(options.enabled(Feature.DRIVING));
        NpcMovementController.VEHICLE.apply(options);
        NpcMovementController.FREE.apply(options);
        assertTrue(options.enabled(Feature.DRIVING));
        assertEquals(NpcMovementController.FREE, NpcMovementController.of(options));
    }

    @Test
    void locomotionClicksAreNotClaimedByAnimationOrCrewHandlers() throws ReflectiveOperationException
    {
        HeldItemAnimationPage animations = mock(HeldItemAnimationPage.class, CALLS_REAL_METHODS);
        CrewPage crew = mock(CrewPage.class, CALLS_REAL_METHODS);
        for (String name : new String[]{"CONTROLLER_ID", "MOVEMENT_SECTION_ID", "MOVEMENT_UP_ID", "MOVEMENT_DOWN_ID"})
        {
            var field = GuiWolffsMod.class.getDeclaredField(name);
            field.setAccessible(true);
            int first = field.getInt(null);
            int count = name.equals("MOVEMENT_SECTION_ID") ? 4 : 1;
            for (int id = first; id < first + count; id++)
            {
                assertFalse(animations.handles(id), name + " is claimed by held-item animation controls");
                assertFalse(crew.handles(id), name + " is claimed by crew controls");
            }
        }
    }
}
