package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.weapons.PilotGun;
import com.flansmodultimate.common.driveables.weapons.ShootPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveableTypeGunBankInferenceTest
{
    private static final ShootPoint GUN_POINT = new ShootPoint(new PilotGun(new Vector3f(), EnumDriveablePart.TURRET, "maschinengewehr34"), new Vector3f());
    private static final ShootPoint PLAIN_POINT = new ShootPoint(new DriveablePosition(new Vector3f(), EnumDriveablePart.TURRET), new Vector3f());

    @Test
    void undeclaredBankMountingGunsBecomesGunBank()
    {
        assertEquals(EnumWeaponType.GUN, DriveableType.inferGunBank(EnumWeaponType.NONE, List.of(GUN_POINT)));
    }

    @Test
    void declaredBankKeepsItsType()
    {
        assertEquals(EnumWeaponType.SHELL, DriveableType.inferGunBank(EnumWeaponType.SHELL, List.of(GUN_POINT)));
    }

    @Test
    void undeclaredBankWithoutGunsStaysNone()
    {
        assertEquals(EnumWeaponType.NONE, DriveableType.inferGunBank(EnumWeaponType.NONE, List.of(PLAIN_POINT)));
        assertEquals(EnumWeaponType.NONE, DriveableType.inferGunBank(EnumWeaponType.NONE, List.of()));
    }
}
