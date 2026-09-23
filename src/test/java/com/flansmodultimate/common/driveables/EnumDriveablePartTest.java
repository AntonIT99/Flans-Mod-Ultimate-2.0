package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumDriveablePartTest
{
    @Test
    void resolvesEveryAdditionalKrishnaPartToItsOwnValue()
    {
        Map<String, EnumDriveablePart> expected = new LinkedHashMap<>();
        expected.put("composite", EnumDriveablePart.COMPOSITE);
        expected.put("composite2", EnumDriveablePart.COMPOSITE_2);
        expected.put("spaced", EnumDriveablePart.SPACED);
        expected.put("compositeL", EnumDriveablePart.COMPOSITE_LEFT);
        expected.put("compositeR", EnumDriveablePart.COMPOSITE_RIGHT);
        expected.put("generic0", EnumDriveablePart.GENERIC_0);
        expected.put("generic9", EnumDriveablePart.GENERIC_9);
        expected.put("russianBias", EnumDriveablePart.BIAS);
        expected.put("bulge", EnumDriveablePart.BULGE);
        expected.put("bulgel", EnumDriveablePart.BULGE_LEFT);
        expected.put("bulger", EnumDriveablePart.BULGE_RIGHT);
        expected.put("midsection", EnumDriveablePart.MIDSECTION);
        expected.put("left", EnumDriveablePart.LEFT);
        expected.put("right", EnumDriveablePart.RIGHT);
        expected.put("beltl", EnumDriveablePart.BELT_LEFT);
        expected.put("beltr", EnumDriveablePart.BELT_RIGHT);
        expected.put("ERA2", EnumDriveablePart.ERA_2);
        expected.put("ERA3", EnumDriveablePart.ERA_3);
        expected.put("infantry", EnumDriveablePart.INFANTRY);
        expected.put("turretarmor", EnumDriveablePart.TURRET_ARMOR);
        expected.put("moreturretarmor", EnumDriveablePart.MORE_TURRET_ARMOR);
        expected.put("turretside", EnumDriveablePart.TURRET_SIDE);
        expected.put("gasbag", EnumDriveablePart.GASBAG);
        expected.put("buoyancy", EnumDriveablePart.BUOYANCY);
        expected.put("shield", EnumDriveablePart.SHIELD);
        expected.put("rightSkirt", EnumDriveablePart.RIGHT_SKIRT);
        expected.put("leftSkirt", EnumDriveablePart.LEFT_SKIRT);
        expected.put("turretSkirt", EnumDriveablePart.TURRET_SKIRT);
        expected.put("weakTrt", EnumDriveablePart.TURRET_WEAK);
        expected.put("weakTrt2", EnumDriveablePart.TURRET_WEAK_2);
        expected.put("engine", EnumDriveablePart.ENGINE);
        expected.put("engine6", EnumDriveablePart.ENGINE_6);
        expected.put("weakSpot", EnumDriveablePart.WEAK_SPOT);
        expected.put("weakSpot2", EnumDriveablePart.WEAK_SPOT_2);
        expected.put("weakSpot3", EnumDriveablePart.WEAK_SPOT_3);

        expected.forEach((name, part) -> assertEquals(part, EnumDriveablePart.getPart(name), name));
        assertEquals(expected.size(), expected.values().stream().distinct().count());
    }
}
