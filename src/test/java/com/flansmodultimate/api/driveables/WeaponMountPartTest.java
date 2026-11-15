package com.flansmodultimate.api.driveables;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponMountPartTest
{
    private static WeaponMount mount(boolean onTurret, String part)
    {
        return new WeaponMount(1, false, onTurret, MountAim.REST, MountAim.REST, 1F, 1F, Vec3.ZERO, Vec3.ZERO, Optional.empty(), Vec3.ZERO, Optional.empty(), part);
    }

    @Test
    void mountWithoutPartSitsInTurretOrHull()
    {
        assertEquals("turret", mount(true, null).part());
        assertEquals("core", mount(false, " ").part());
        assertEquals("core", new WeaponMount(1, false, false, MountAim.REST, MountAim.REST, 1F, 1F, Vec3.ZERO, Vec3.ZERO, Optional.empty()).part());
    }

    @Test
    void occupantsDieOnlyWithTheTurretTheySitIn()
    {
        assertTrue(FlansDriveables.diesWithPart(mount(true, "turret"), "turret"));
        assertTrue(FlansDriveables.diesWithPart(mount(true, "barrel"), "turret"));
        assertTrue(FlansDriveables.diesWithPart(mount(false, "turret3"), "turret3"));
        assertFalse(FlansDriveables.diesWithPart(mount(false, "turret3"), "turret"));
        assertFalse(FlansDriveables.diesWithPart(mount(false, "turret4"), "turret3"));
        assertFalse(FlansDriveables.diesWithPart(mount(false, "core"), "turret"));
        assertFalse(FlansDriveables.diesWithPart(mount(false, "leftWing"), "leftWing"));
    }
}
