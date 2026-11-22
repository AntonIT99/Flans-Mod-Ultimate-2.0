package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Labjac fork's service ceiling for aircraft. */
class FlightCeilingTest
{
    private static final ContentPack PACK = new ContentPack("test", Path.of("build", "test-packs", "flight-ceiling"));

    @Test
    void pushesTheNoseDownOnlyNearTheCeiling()
    {
        assertEquals(-19F, FlightCeiling.limitPitch(-20F, 995D, 1000F));
        assertEquals(-20F, FlightCeiling.limitPitch(-20F, 989D, 1000F), "below the margin the pilot keeps the nose up");
        assertEquals(5F, FlightCeiling.limitPitch(5F, 1200D, 1000F), "a nose already down is left alone");
        assertEquals(0F, FlightCeiling.limitPitch(-0.4F, 1200D, 1000F), "the push levels the nose, never past it");
        assertEquals(-20F, FlightCeiling.limitPitch(-20F, 5000D, 0F), "no ceiling");
        assertFalse(FlightCeiling.atCeiling(Double.NaN, 1000F));
    }

    @Test
    void readsTheCeilingAndIgnoresBadValues()
    {
        assertEquals(0F, plane("ShortName noCeiling").getFlightCeiling());
        assertEquals(750F, plane("ShortName ceiling", "flightCeiling 750").getFlightCeiling());
        assertEquals(0F, plane("ShortName badCeiling", "flightCeiling -5").getFlightCeiling());
    }

    private static PlaneType plane(String... lines)
    {
        PlaneType type = new PlaneType();
        type.load(new TypeFile("ceilingPlane", EnumType.PLANE, PACK, List.of(lines)));
        return type;
    }
}
