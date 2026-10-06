package com.flansmodultimate.common.types;

import com.flansmodultimate.config.Category;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaneTypeGroundSteeringTest
{
    @Test
    void categoryTaxiRateDoesNotReplaceAuthoredFlightModifiers()
    {
        TypeFile file = file("TurnLeftSpeed 0.25", "TurnRightSpeed 0.5");
        Category category = new Category(EnumType.PLANE, "Test plane");
        category.setProperties(Map.of("RealTurnRateDegPerSec", List.of("18")));
        file.addCategoryConfigMap(category, "taxi");
        PlaneType type = new PlaneType();
        type.read(file);
        assertEquals(18F, type.getRealTurnRateDegPerSec());
        assertEquals(0.25F, type.getTurnLeftModifier());
        assertEquals(0.5F, type.getTurnRightModifier());
    }

    @Test
    void ratesUseTheSameAliasAndValidityRulesAsVehicles()
    {
        assertEquals(12F, plane("turnratedegpersec 12").getRealTurnRateDegPerSec());
        assertEquals(14F, plane("TurnRateDegPerSec 12", "RealTurnRateDegPerSec 14").getRealTurnRateDegPerSec());
        for (String value : List.of("0", "-1", "NaN", "Infinity"))
            assertEquals(0F, plane("RealTurnRateDegPerSec " + value).getRealTurnRateDegPerSec());
        assertEquals(0F, plane().getRealTurnRateDegPerSec());
    }

    private static PlaneType plane(String... lines)
    {
        PlaneType type = new PlaneType();
        type.read(file(lines));
        return type;
    }

    private static TypeFile file(String... lines)
    {
        List<String> definition = new ArrayList<>(List.of("Driver 0 0 0"));
        definition.addAll(List.of(lines));
        return new TypeFile("taxi", EnumType.PLANE, new ContentPack("test", Path.of("build", "test-packs", "test")), definition);
    }
}
