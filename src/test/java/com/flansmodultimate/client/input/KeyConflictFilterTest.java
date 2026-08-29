package com.flansmodultimate.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.junit.jupiter.api.Test;

import net.minecraft.client.KeyMapping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pairings that need no live client. The vanilla rules are left to
 * play testing, since they compare against fields on a running Options.
 */
class KeyConflictFilterTest
{
    private static final int ANY_KEY = InputConstants.KEY_R;

    @Test
    void aDriveableBindNeverClashesWithFlansModReloaded()
    {
        KeyMapping ours = mapping("key.flansmodultimate.plane.control_mode",
            EnumKeyConflictContext.PLANE, "key.categories.flansmodultimate.planes");

        // Matched on the category it registers, and on its name, so a rename of
        // either one on their side still leaves the other recognisable.
        assertTrue(KeyConflictFilter.cannotOverlap(ours,
            mapping("key.flansmod.mode_toggle", KeyConflictContext.IN_GAME, "key.categories.flansmod")));
        assertTrue(KeyConflictFilter.cannotOverlap(ours,
            mapping("key.flansmod.manual_reload", KeyConflictContext.IN_GAME, "key.categories.moved")));
    }

    @Test
    void theFilterIsSymmetric()
    {
        KeyMapping ours = mapping("key.flansmodultimate.vehicle.left",
            EnumKeyConflictContext.GROUND_DRIVEABLE, "key.categories.flansmodultimate.vehicles");
        KeyMapping reloaded = mapping("key.flansmod.yaw_left", KeyConflictContext.IN_GAME, "key.categories.flansmod");

        assertTrue(KeyConflictFilter.cannotOverlap(ours, reloaded));
        assertTrue(KeyConflictFilter.cannotOverlap(reloaded, ours));
    }

    @Test
    void twoMappingsWeKnowNothingAboutAreLeftAlone()
    {
        KeyMapping reloaded = mapping("key.flansmod.manual_reload", KeyConflictContext.IN_GAME, "key.categories.flansmod");
        KeyMapping other = mapping("key.somemod.something", KeyConflictContext.IN_GAME, "key.categories.somemod");

        assertFalse(KeyConflictFilter.cannotOverlap(reloaded, other));
    }

    @Test
    void aPlaneBindAndAGroundBindMayShareAKey()
    {
        KeyMapping plane = mapping("key.flansmodultimate.plane.roll_left",
            EnumKeyConflictContext.PLANE, "key.categories.flansmodultimate.planes");
        KeyMapping ground = mapping("key.flansmodultimate.vehicle.left",
            EnumKeyConflictContext.GROUND_DRIVEABLE, "key.categories.flansmodultimate.vehicles");

        assertTrue(KeyConflictFilter.cannotOverlap(plane, ground));
    }

    @Test
    void bindsSharedByEveryDriveableStillClashWithAPlaneBind()
    {
        KeyMapping plane = mapping("key.flansmodultimate.plane.gear",
            EnumKeyConflictContext.PLANE, "key.categories.flansmodultimate.planes");
        KeyMapping anyDriveable = mapping("key.flansmodultimate.driveable.door",
            EnumKeyConflictContext.DRIVEABLE, "key.categories.flansmodultimate.driveables");

        assertFalse(KeyConflictFilter.cannotOverlap(plane, anyDriveable));
    }

    @Test
    void driveableWeaponBindsDoNotReportTheirVanillaMouseDefaultsAsConflicts()
    {
        KeyMapping primary = mapping("key.flansmodultimate.driveable.primary",
            EnumKeyConflictContext.DRIVEABLE, "key.categories.flansmodultimate.driveables");
        KeyMapping secondaryAlternative = mapping("key.flansmodultimate.driveable.secondary_alternative",
            EnumKeyConflictContext.DRIVEABLE, "key.categories.flansmodultimate.driveables");

        assertTrue(KeyConflictFilter.cannotOverlap(primary,
            mapping("key.attack", KeyConflictContext.UNIVERSAL, "key.categories.gameplay")));
        assertTrue(KeyConflictFilter.cannotOverlap(secondaryAlternative,
            mapping("key.use", KeyConflictContext.UNIVERSAL, "key.categories.gameplay")));
    }

    @Test
    void otherDriveableBindsStillReportAConflictWithAttack()
    {
        KeyMapping door = mapping("key.flansmodultimate.driveable.door",
            EnumKeyConflictContext.DRIVEABLE, "key.categories.flansmodultimate.driveables");

        assertFalse(KeyConflictFilter.cannotOverlap(door,
            mapping("key.attack", KeyConflictContext.UNIVERSAL, "key.categories.gameplay")));
    }

    private static KeyMapping mapping(String name, IKeyConflictContext context, String category)
    {
        return new KeyMapping(name, context, InputConstants.Type.KEYSYM, ANY_KEY, category);
    }
}
