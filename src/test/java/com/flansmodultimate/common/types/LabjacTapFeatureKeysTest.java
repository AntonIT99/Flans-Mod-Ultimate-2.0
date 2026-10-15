package com.flansmodultimate.common.types;

import com.flansmodultimate.common.guns.TracerBeam;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The opt-in Labjac Edition (TaP) keys for gun screen shake, tracer beams, mixed belts, hold-to-throw grenades
 * and thermal scopes. Definitions without them keep their previous behaviour.
 */
class LabjacTapFeatureKeysTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "tap"));
    private static final float EPSILON = 1.0E-6F;

    @Test
    void gunScreenShakeIsOptIn()
    {
        GunType plain = gun();
        assertFalse(plain.isHasScreenShake());
        assertTrue(plain.isScreenShakeUsesSustainedRecoil());
        assertEquals(1F, plain.getScreenShakeIntensity(), EPSILON);

        GunType shaking = gun("HasScreenShake true", "ScreenShakeStyle Clean", "ScreenShakeIntensity 2.5", "CameraRecoil 1.5", "ScreenShakeCameraKick false");
        assertTrue(shaking.isHasScreenShake());
        assertFalse(shaking.isScreenShakeUsesSustainedRecoil());
        assertEquals(2.5F, shaking.getScreenShakeIntensity(), EPSILON);
        assertEquals(1.5F, shaking.getCameraRecoil(), EPSILON);
        assertFalse(shaking.isScreenShakeCameraKick());
    }

    @Test
    void cameraRecoilDefaultsToTheVerticalFancyRecoil()
    {
        assertEquals(0.75F, gun("FancyRecoil 0.75 0.2").getCameraRecoil(), EPSILON);
        assertEquals(2F, gun("FancyRecoil 0.75 0.2", "CameraRecoil 2").getCameraRecoil(), EPSILON);
        assertEquals(0F, gun().getCameraRecoil(), EPSILON);
    }

    @Test
    void tracerBeamsAreReadWithTheLabjacDefaults()
    {
        BulletType plain = bullet();
        assertFalse(plain.getTracerBeam().enabled());
        assertNull(plain.tracerBeamFor(false));

        BulletType tracer = bullet("TracerBeam true", "TracerBeamColor 1 0.5 0", "TracerBeamLength 6", "TracerBeamWidth 0.05", "TracerBeamAlpha 2");
        TracerBeam beam = tracer.tracerBeamFor(false);
        assertNotNull(beam);
        assertEquals(1F, beam.red(), EPSILON);
        assertEquals(0.5F, beam.green(), EPSILON);
        assertEquals(0F, beam.blue(), EPSILON);
        assertEquals(6F, beam.length(), EPSILON);
        assertEquals(0.05F, beam.width(), EPSILON);
        assertEquals(1F, beam.alpha(), EPSILON, "alpha is clamped to 1");

        TracerBeam defaults = bullet("TracerBeam true").tracerBeamFor(false);
        assertEquals(TracerBeam.DEFAULT.length(), defaults.length(), EPSILON);
        assertEquals(TracerBeam.DEFAULT.green(), defaults.green(), EPSILON);
    }

    @Test
    void fastRoundsDrawLongerBeamsUpToThreefold()
    {
        assertEquals(1F, TracerBeam.speedScale(10D), EPSILON);
        assertEquals(1F, TracerBeam.speedScale(15D), EPSILON);
        assertEquals((float) Math.sqrt(2D), TracerBeam.speedScale(30D), EPSILON);
        assertEquals(3F, TracerBeam.speedScale(1000D), EPSILON);
        assertEquals(1F, TracerBeam.speedScale(Double.NaN), EPSILON);
    }

    @Test
    void everyNthRoundOfAMixedBeltIsAnAlternateRound()
    {
        BulletType belt = bullet("HasAlternateModel true", "AlternateBulletLoad 5", "AlternateTracerBeam true", "AlternateTracerBeamColor 1 0 0");
        assertFalse(belt.isAlternateRound(0));
        assertFalse(belt.isAlternateRound(3));
        assertTrue(belt.isAlternateRound(4), "the fifth round fired from the item");
        assertTrue(belt.isAlternateRound(9));
        assertFalse(belt.isAlternateRound(-1));

        assertNull(belt.tracerBeamFor(false), "ordinary rounds of this belt draw no beam");
        assertEquals(1F, belt.tracerBeamFor(true).red(), EPSILON);
    }

    @Test
    void alternateRoundsNeedHasAlternateModelAsInTheLabjacEdition()
    {
        assertFalse(bullet("AlternateBulletLoad 5").isAlternateRound(4));
        assertFalse(bullet("HasAlternateModel true", "AlternateBulletLoad 1").isAlternateRound(0));
    }

    @Test
    void alternateRoundsWithoutTheirOwnBeamKeepTheOrdinaryOne()
    {
        BulletType belt = bullet("HasAlternateModel true", "AlternateBulletLoad 2", "TracerBeam true");
        assertSame(belt.getTracerBeam(), belt.tracerBeamFor(true));
    }

    @Test
    void holdToThrowIsOptInAndOnlyItDefaultsAStopSpeed()
    {
        GrenadeType plain = grenade();
        assertFalse(plain.isHoldToThrow());
        assertEquals(0F, plain.getGrenadeStopSpeed(), EPSILON);
        assertEquals(1F, plain.getGrenadeGroundFriction(), EPSILON);
        assertEquals(1F, plain.getGrenadeAirDrag(), EPSILON);

        GrenadeType held = grenade("HoldToThrow true", "OverhandThrowSpeedMultiplier 1.2", "UnderhandThrowSpeedMultiplier 0.5", "GrenadeGroundFriction 0.85",
            "GrenadeAirDrag 0.99");
        assertTrue(held.isHoldToThrow());
        assertEquals(1.2F, held.getOverhandThrowSpeedMultiplier(), EPSILON);
        assertEquals(0.5F, held.getUnderhandThrowSpeedMultiplier(), EPSILON);
        assertEquals(0.85F, held.getGrenadeGroundFriction(), EPSILON);
        assertEquals(0.99F, held.getGrenadeAirDrag(), EPSILON);
        assertEquals(0.03F, held.getGrenadeStopSpeed(), EPSILON);
        assertEquals(0.1F, grenade("HoldToThrow true", "GrenadeStopSpeed 0.1").getGrenadeStopSpeed(), EPSILON);
    }

    @Test
    void theLabjacImpactDetonationNamesAreAccepted()
    {
        assertTrue(grenade("CringeExplodeOnImpact true").isExplodeOnImpact());
        assertTrue(grenade("CringeDetonateOnImpact true").isExplodeOnImpact());
        assertFalse(grenade().isExplodeOnImpact());
    }

    @Test
    void thermalScopesAreOptIn()
    {
        assertFalse(gun().hasThermalVision());
        assertTrue(gun("HasThermalVision true").hasThermalVision());
        assertTrue(gun("HasThermal true").hasThermalVision());
        assertTrue(attachment("HasThermalVision true").hasThermalVision());
        assertFalse(attachment("HasNightVision true").hasThermalVision(), "night vision alone is not thermal");
    }

    private static GunType gun(String... lines)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testGun", EnumType.GUN, PACK, List.of(lines)));
        return type;
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.read(new TypeFile("testBullet", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }

    private static GrenadeType grenade(String... lines)
    {
        GrenadeType type = new GrenadeType();
        type.read(new TypeFile("testGrenade", EnumType.GRENADE, PACK, List.of(lines)));
        return type;
    }

    private static AttachmentType attachment(String... lines)
    {
        AttachmentType type = new AttachmentType();
        type.read(new TypeFile("testScope", EnumType.ATTACHMENT, PACK, List.of(lines)));
        return type;
    }
}
