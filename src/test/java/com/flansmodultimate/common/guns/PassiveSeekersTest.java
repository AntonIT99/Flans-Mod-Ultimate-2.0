package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Acoustic torpedoes and anti-radiation seekers ported from the Labjac fork. */
class PassiveSeekersTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "passive-seekers"));

    @Test
    void throttleIsNoiseAndAsternIsHalfAsLoud()
    {
        assertEquals(0F, PassiveSeekers.noise(0F));
        assertEquals(0.5F, PassiveSeekers.noise(0.5F));
        assertEquals(1F, PassiveSeekers.noise(3F));
        assertEquals(0.25F, PassiveSeekers.noise(-0.5F));
        assertEquals(0F, PassiveSeekers.noise(Float.NaN));
    }

    @Test
    void hearingGrowsFromTheMinimumRangeWithNoise()
    {
        assertEquals(5D, PassiveSeekers.hearingRange(5F, 250F, 0F));
        assertEquals(255D, PassiveSeekers.hearingRange(5F, 250F, 1F));
        assertEquals(67.5D, PassiveSeekers.hearingRange(5F, 250F, -0.5F));
        assertTrue(PassiveSeekers.hears(4.9D * 4.9D, 5F, 250F, 0F));
        assertFalse(PassiveSeekers.hears(6D * 6D, 5F, 250F, 0F), "a stopped hull is silent beyond the minimum range");
        assertTrue(PassiveSeekers.hears(100D * 100D, 5F, 250F, 0.5F));
        assertFalse(PassiveSeekers.hears(Double.NaN, 5F, 250F, 1F));
    }

    @Test
    void modernTorpedoesRunAsTorpedoesWithForkDefaults()
    {
        BulletType plain = bullet();
        assertFalse(plain.isModernTorpedo());
        assertFalse(plain.isTorpedo());
        assertFalse(plain.isAntiRadiation());

        BulletType modern = bullet("modernTorpedo True");
        assertTrue(modern.isModernTorpedo());
        assertTrue(modern.isTorpedo(), "a modern torpedo needs no Torpedo line of its own");
        assertEquals(5F, modern.getAswMinRange());
        assertEquals(250F, modern.getSeekerRange());

        BulletType tuned = bullet("modernTorpedo true", "ASWminRange 12", "seekerRange -3");
        assertEquals(12F, tuned.getAswMinRange());
        assertEquals(0F, tuned.getSeekerRange());

        assertTrue(bullet("antiRadiation true").isAntiRadiation());
    }

    @Test
    void eitherRadarKeyGivesADriveableARadar()
    {
        assertFalse(vehicle("ShortName noRadar").isRadar());
        assertTrue(vehicle("ShortName groundRadar", "hasRadar true").isRadar());
        PlaneType plane = new PlaneType();
        plane.load(new TypeFile("planeRadar", EnumType.PLANE, PACK, List.of("ShortName planeRadar", "hasPlaneRadar true")));
        assertTrue(plane.isRadar());
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("seeker", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }

    private static VehicleType vehicle(String... lines)
    {
        VehicleType type = new VehicleType();
        type.load(new TypeFile("radar", EnumType.VEHICLE, PACK, List.of(lines)));
        return type;
    }
}
