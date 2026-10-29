package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveableRepairRulesTest
{
    @Test
    void toolHealingScalesWithThePartButNeverExceedsItsDefinition()
    {
        assertEquals(5F, DriveableRepairRules.toolAmount(100F, 100));
        assertEquals(50F, DriveableRepairRules.toolAmount(1000F, 100));
        assertEquals(100F, DriveableRepairRules.toolAmount(10000F, 100));
        assertEquals(1F, DriveableRepairRules.toolAmount(1F, 100));
    }

    @Test
    void repairInputIsAnEdgeActionAndSupportsLabjacsLegacyKey()
    {
        assertEquals(DriveableInput.REPAIR, DriveableInput.forLegacyKey(34));
        assertEquals(DriveableInput.REPAIR, DriveableInput.sanitize(DriveableInput.REPAIR));
        assertEquals(DriveableInput.REPAIR, DriveableInput.EDGE_TRIGGERED_MASK & DriveableInput.REPAIR);
        assertEquals(0, DriveableInput.CONTINUOUS_MASK & DriveableInput.REPAIR);
    }

    @Test
    void ordinaryRebuildsLeaveWorkForToolsButDefensiveEquipmentIsFullyRestored()
    {
        assertEquals(115F, DriveableRepairRules.rebuildHealth(EnumDriveablePart.CORE, 1150F));
        assertEquals(0.5F, DriveableRepairRules.rebuildHealth(EnumDriveablePart.CORE, 0.5F));
        for (EnumDriveablePart type : new EnumDriveablePart[]{EnumDriveablePart.ERA, EnumDriveablePart.ERA_2, EnumDriveablePart.ERA_3, EnumDriveablePart.APS, EnumDriveablePart.ADS})
            assertEquals(1150F, DriveableRepairRules.rebuildHealth(type, 1150F));
    }

    @Test
    void mobilityPartsRetainFasterRecoveryThanOrdinaryParts()
    {
        assertEquals(15F, DriveableRepairRules.timedAmount(EnumDriveablePart.CORE, 1000F, false));
        assertEquals(25F, DriveableRepairRules.timedAmount(EnumDriveablePart.LEFT_TRACK, 1000F, false));
        assertEquals(25F, DriveableRepairRules.timedAmount(EnumDriveablePart.FRONT_LEFT_WHEEL, 1000F, false));
        assertEquals(25F, DriveableRepairRules.timedAmount(EnumDriveablePart.CORE, 1000F, true));
        assertEquals(0.25F, DriveableRepairRules.timedAmount(EnumDriveablePart.CORE, 1F, false));
    }

    @Test
    void crewLossSlowsShipRepairsAndFloodRecoveryWithoutDivisionByZero()
    {
        assertEquals(20, DriveableRepairRules.shipInterval(1F, 1F));
        assertEquals(60, DriveableRepairRules.shipInterval(0F, 0F));
        assertEquals(60, DriveableRepairRules.buoyancyInterval(0F));
        assertEquals(20, DriveableRepairRules.buoyancyInterval(1F));
        assertTrue(DriveableRepairRules.shipInterval(1F, 0F) < DriveableRepairRules.shipInterval(0F, 0F));
        assertEquals(60, DriveableRepairRules.shipInterval(Float.NaN, -1F));
        assertEquals(20, DriveableRepairRules.buoyancyInterval(2F));
    }
}
