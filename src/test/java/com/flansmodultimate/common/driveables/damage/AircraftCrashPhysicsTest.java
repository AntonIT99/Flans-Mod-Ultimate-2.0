package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.api.driveables.CrashMode;
import com.flansmodultimate.api.driveables.CrashMotion;
import com.flansmodultimate.api.driveables.DriveableAttitude;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

/** Pins how a crippled or dead aircraft comes down, and the attitude arithmetic both driveables and NPCs rely on. */
class AircraftCrashPhysicsTest
{
    private static final double GRAVITY = 0.08D;

    /** Flies the fall for the given ticks and returns the final attitude, with the motion left in the holder. */
    private static DriveableAttitude fly(DriveableAttitude start, CrashMotion[] motion, CrashMode mode, float lift, int ticks)
    {
        DriveableAttitude attitude = start;
        for (int tick = 0; tick < ticks; tick++)
        {
            motion[0] = AircraftCrashPhysics.step(motion[0], attitude, mode, lift, GRAVITY, false);
            attitude = AircraftCrashPhysics.rotate(attitude, motion[0].rotation());
        }
        return attitude;
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 0", "90, 20, 0", "-135, -30, 40", "30, 60, -70", "170, -80, 120"})
    @DisplayName("Angles turn into an attitude and back")
    void anglesRoundTrip(float heading, float pitch, float bank)
    {
        DriveableAttitude attitude = DriveableAttitude.of(heading, pitch, bank);
        assertEquals(1D, attitude.front().length(), 1.0E-9D);
        assertEquals(1D, attitude.roof().length(), 1.0E-9D);
        assertEquals(0D, attitude.front().dot(attitude.roof()), 1.0E-9D);
        assertEquals(pitch, attitude.pitch(), 1.0E-3F);
        assertEquals(0F, wrap(heading - attitude.heading()), 1.0E-3F);
        assertEquals(0F, wrap(bank - attitude.roll()), 1.0E-3F);
        if (pitch == 0F)
            assertEquals(attitude.roll(), attitude.bank(), 1.0E-3F, "level, bank and roll agree");
    }

    @Test
    @DisplayName("A rotation about the nose rolls the right wing down, one about the right axis lifts the nose")
    void rotationSenses()
    {
        DriveableAttitude level = DriveableAttitude.of(0F, 0F, 0F);
        Vec3 right = level.front().cross(level.roof());
        assertTrue(AircraftCrashPhysics.rotate(level, level.front().scale(10D)).bank() > 9F);
        assertTrue(AircraftCrashPhysics.rotate(level, right.scale(10D)).pitch() > 9F);
        assertSame(level, AircraftCrashPhysics.rotate(level, Vec3.ZERO));
    }

    @Test
    @DisplayName("A dead aircraft follows its own path down nose first, rather than tumbling like a brick")
    void diveFollowsThePath()
    {
        DriveableAttitude level = DriveableAttitude.of(0F, 0F, 0F);
        CrashMotion[] motion = {CrashMotion.of(new Vec3(0D, 0D, 1.5D))};
        DriveableAttitude end = fly(level, motion, CrashMode.DIVE, 1F, 80);
        Vec3 path = motion[0].velocity().normalize();
        assertTrue(motion[0].velocity().y < -0.3D, "it is coming down");
        assertTrue(end.front().dot(path) > Math.cos(Math.toRadians(25D)), "its nose points along its path");
        assertTrue(end.pitch() < -20F, "its nose has gone down");
        assertEquals(0F, end.roll(), 5F, "a dive does not roll by itself");
    }

    @Test
    @DisplayName("Losing a wing rolls the aircraft over toward the stump")
    void lostWingSpirals()
    {
        DriveableAttitude level = DriveableAttitude.of(0F, 0F, 0F);
        CrashMotion[] right = {CrashMotion.of(new Vec3(0D, 0D, 1.5D))};
        CrashMotion[] left = {CrashMotion.of(new Vec3(0D, 0D, 1.5D))};
        assertTrue(fly(level, right, CrashMode.SPIRAL_RIGHT, 0.5F, 12).bank() > 10F);
        assertTrue(fly(level, left, CrashMode.SPIRAL_LEFT, 0.5F, 12).bank() < -10F);
    }

    @Test
    @DisplayName("A flat spin turns fast about the vertical, loses its forward speed and falls slowly, almost level")
    void flatSpinFallsLevel()
    {
        DriveableAttitude level = DriveableAttitude.of(0F, 0F, 0F);
        CrashMotion[] motion = {CrashMotion.of(new Vec3(0D, 0D, 1.5D))};
        DriveableAttitude end = fly(level, motion, CrashMode.FLAT_SPIN, 1F, 200);
        Vec3 velocity = motion[0].velocity();
        assertTrue(velocity.horizontalDistance() < 0.05D, "the spin has eaten its forward speed");
        assertEquals(-AircraftCrashPhysics.FLAT_SPIN_TERMINAL, velocity.y, 1.0E-6D, "it falls at the flat spin's slow terminal speed");
        assertTrue(Math.abs(motion[0].rotation().y) > AircraftCrashPhysics.FLAT_SPIN_RATE * 0.9D, "it spins about the vertical");
        assertTrue(end.pitch() < 0F && end.pitch() > -30F, "its nose stays a little below the horizon");
        assertEquals(0F, end.roll(), 10F);
    }

    @Test
    @DisplayName("A helicopter without its tail rotor spins about its mast, slowed by what its rotor still lifts")
    void mastSpinFallsSlowerWithLift()
    {
        DriveableAttitude level = DriveableAttitude.of(0F, 0F, 0F);
        CrashMotion[] lifting = {CrashMotion.of(Vec3.ZERO)};
        CrashMotion[] dead = {CrashMotion.of(Vec3.ZERO)};
        fly(level, lifting, CrashMode.MAST_SPIN, 1F, 30);
        fly(level, dead, CrashMode.MAST_SPIN, 0F, 30);
        assertTrue(lifting[0].velocity().y > dead[0].velocity().y, "a turning rotor slows the fall");
        assertTrue(lifting[0].rotation().length() > AircraftCrashPhysics.MAST_SPIN_RATE * 0.9D);
        CrashMotion mirrored = AircraftCrashPhysics.step(CrashMotion.of(Vec3.ZERO), level, CrashMode.MAST_SPIN, 1F, GRAVITY, true);
        CrashMotion plain = AircraftCrashPhysics.step(CrashMotion.of(Vec3.ZERO), level, CrashMode.MAST_SPIN, 1F, GRAVITY, false);
        assertEquals(-plain.rotation().y, mirrored.rotation().y, 1.0E-9D, "a mirrored aircraft spins the other way");
    }

    @Test
    @DisplayName("However much wing is left, a falling aircraft always comes down")
    void wingsNeverHoldItUp()
    {
        DriveableAttitude level = DriveableAttitude.of(0F, 0F, 0F);
        CrashMotion[] motion = {CrashMotion.of(new Vec3(0D, 0D, 2D))};
        double start = 0D;
        double height = start;
        DriveableAttitude attitude = level;
        for (int tick = 0; tick < 200; tick++)
        {
            motion[0] = AircraftCrashPhysics.step(motion[0], attitude, CrashMode.DIVE, 1F, GRAVITY, false);
            attitude = AircraftCrashPhysics.rotate(attitude, motion[0].rotation());
            height += motion[0].velocity().y;
            assertTrue(motion[0].velocity().y >= -AircraftCrashPhysics.DIVE_TERMINAL - 1.0E-9D);
        }
        assertTrue(height < start - 50D);
    }

    @Test
    @DisplayName("A tumble keeps its own spin and loses it slowly")
    void tumbleKeepsItsSpin()
    {
        Vec3 spin = new Vec3(3D, -2D, 5D);
        CrashMotion next = AircraftCrashPhysics.step(new CrashMotion(Vec3.ZERO, spin), DriveableAttitude.of(0F, 0F, 0F), CrashMode.TUMBLE, 1F, GRAVITY, false);
        assertEquals(spin.scale(AircraftCrashPhysics.TUMBLE_RETENTION), next.rotation());
        assertEquals(-GRAVITY * AircraftCrashPhysics.AIR_RETENTION, next.velocity().y, 1.0E-9D, "nothing holds a tumbling wreck up");
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 0", "45, 30, 20", "-120, -60, -45", "10, 80, 0"})
    @DisplayName("Euler angles kept in any convention are found for a world-space turn")
    void solvesAnglesForATurn(float yaw, float pitch, float roll)
    {
        // A convention unlike the attitude's own: pitch counted nose down, as Minecraft does, and roll to the left.
        AircraftCrashPhysics.AttitudeOf convention = (y, p, r) -> DriveableAttitude.of(y, -p, -r);
        AircraftCrashPhysics.Angles start = new AircraftCrashPhysics.Angles(yaw, pitch, roll);
        Vec3 rotation = new Vec3(2D, -3D, 1.5D);
        DriveableAttitude wanted = AircraftCrashPhysics.rotate(convention.at(yaw, pitch, roll), rotation);
        AircraftCrashPhysics.Angles turned = AircraftCrashPhysics.turn(convention, start, rotation);
        DriveableAttitude reached = convention.at(turned.yaw(), turned.pitch(), turned.roll());
        assertTrue(reached.front().distanceTo(wanted.front()) < 2.0E-3D, "the nose ends where the turn puts it");
        assertTrue(reached.roof().distanceTo(wanted.roof()) < 2.0E-3D, "and so does the roof");
        assertSame(start, AircraftCrashPhysics.turn(convention, start, Vec3.ZERO));
    }

    private static float wrap(float degrees)
    {
        float wrapped = degrees % 360F;
        if (wrapped >= 180F)
            wrapped -= 360F;
        if (wrapped < -180F)
            wrapped += 360F;
        return wrapped;
    }
}
