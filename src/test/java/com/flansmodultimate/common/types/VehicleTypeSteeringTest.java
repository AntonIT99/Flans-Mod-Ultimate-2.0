package com.flansmodultimate.common.types;

import com.flansmodultimate.config.Category;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTypeSteeringTest
{
    @Test
    void categoryRateAppliesOverAnExistingLegacyDefinition()
    {
        TypeFile file = new TypeFile("steering", EnumType.VEHICLE, new ContentPack("test", Path.of("build", "test-packs", "test")),
            List.of("Driver 0 0 0", "Tank true", "TurnLeftSpeed 0.35", "TurnRightSpeed 0.35"));
        Category category = new Category(EnumType.VEHICLE, "Test tank");
        category.setProperties(Map.of("RealTurnRateDegPerSec", List.of("12")));
        file.addCategoryConfigMap(category, "steering");
        VehicleType type = new VehicleType();
        type.read(file);
        assertTrue(type.usesRealTurnRate(false, false, type.isTank()));
        assertEquals(12F, type.getRealTurnRateDegPerSec());
        assertEquals(0.35F, type.getTurnLeftModifier());
    }

    @Test
    void realRateOverridesLegacySteeringWithoutRequiringAPropulsionProfile()
    {
        VehicleType type = vehicle("RealTurnRateDegPerSec 12", "TurnLeftSpeed 0.01", "TurnRightSpeed 2");
        assertEquals(12F, type.getRealTurnRateDegPerSec());
        assertTrue(type.usesRealTurnRate(false, false, true));
        assertFalse(type.usesRealTurnRate(true, false, true), "forced legacy retains its original formula");
        assertFalse(type.usesRealTurnRate(false, true, true), "crew-pushed steering stays separate");
        assertFalse(type.getResolvedPhysics().hasGroundPropulsion());
        assertEquals(0.01F, type.getTurnLeftModifier());
        assertEquals(2F, type.getTurnRightModifier());
    }

    @Test
    void rollingRateRequiresARealReferenceSpeed()
    {
        assertFalse(vehicle("RealTurnRateDegPerSec 12").usesRealTurnRate(false, false, false));
        assertTrue(vehicle("RealTurnRateDegPerSec 12", "RealMaxSpeedKmh 38").usesRealTurnRate(false, false, false));
    }

    @Test
    void absentInvalidAndAliasedValuesPreserveCompatibility()
    {
        assertFalse(vehicle().usesRealTurnRate(false, false, true));
        for (String value : List.of("0", "-1", "NaN", "Infinity"))
            assertFalse(vehicle("RealTurnRateDegPerSec " + value).usesRealTurnRate(false, false, true));
        assertEquals(12F, vehicle("turnratedegpersec 12").getRealTurnRateDegPerSec());
        assertEquals(14F, vehicle("TurnRateDegPerSec 12", "RealTurnRateDegPerSec 14").getRealTurnRateDegPerSec());
        assertEquals(0F, vehicle("TurnRateDegPerSec 12", "RealTurnRateDegPerSec NaN").getRealTurnRateDegPerSec());
    }

    private static VehicleType vehicle(String... lines)
    {
        List<String> definition = new ArrayList<>(List.of("Driver 0 0 0"));
        definition.addAll(List.of(lines));
        VehicleType type = new VehicleType();
        type.read(new TypeFile("steering", EnumType.VEHICLE, new ContentPack("test", Path.of("build", "test-packs", "test")), definition));
        return type;
    }
}
