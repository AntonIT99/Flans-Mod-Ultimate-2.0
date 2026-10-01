package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriveablePartExplosionEligibilityTest
{
    @Test
    void destroyedPartCannotBeSelectedForAnotherExplosionUntilRepaired()
    {
        DriveablePart wing = new DriveablePart(EnumDriveablePart.LEFT_WING,
            new CollisionBox(100F, 0F, 0F, 0F, 16F, 16F, 16F));

        assertTrue(wing.canReceiveExplosionDamage());
        wing.damage(100F, false);
        assertFalse(wing.canReceiveExplosionDamage());
        wing.repair(1F);
        assertTrue(wing.canReceiveExplosionDamage());
    }

    @Test
    void structuralPartWithoutHealthDoesNotConsumeExplosionDamage()
    {
        DriveablePart structural = new DriveablePart(EnumDriveablePart.TAIL,
            new CollisionBox(0F, 0F, 0F, 0F, 16F, 16F, 16F));

        assertFalse(structural.canReceiveExplosionDamage());
    }
}
