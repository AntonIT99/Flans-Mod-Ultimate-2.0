package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaneTypeAirBrakeTest
{
    @Test
    void strengthDefaultsToOneAndAcceptsZeroAndPositiveValues()
    {
        assertEquals(1F, plane().getAirBrakeStrengthMultiplier());
        assertEquals(0F, plane("AirBrakeStrengthMultiplier 0").getAirBrakeStrengthMultiplier());
        assertEquals(2.5F, plane("airbrakestrengthmultiplier 2.5").getAirBrakeStrengthMultiplier());
    }

    @Test
    void invalidStrengthFallsBackToOne()
    {
        for (String value : List.of("-1", "NaN", "Infinity", "invalid"))
            assertEquals(1F, plane("AirBrakeStrengthMultiplier " + value).getAirBrakeStrengthMultiplier());
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
