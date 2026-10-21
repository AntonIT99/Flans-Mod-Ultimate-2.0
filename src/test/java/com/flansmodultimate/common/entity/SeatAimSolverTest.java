package com.flansmodultimate.common.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class SeatAimSolverTest
{
    /** Degrees between two directions. */
    private static double angleBetween(Vec3 first, Vec3 second)
    {
        double cosine = first.normalize().dot(second.normalize());
        return Math.acos(Mth.clamp(cosine, -1D, 1D)) * Mth.RAD_TO_DEG;
    }

    /** A driveable turned, rolled and pitched in the world, whose seat aim may run the opposite way in pitch, as a plane's does. */
    private static SeatAimSolver.AimDirection driveable(float heading, float roll, float tilt, boolean pitchFlipped)
    {
        return (yaw, pitch) -> Vec3.directionFromRotation(pitchFlipped ? -pitch : pitch, yaw).zRot(roll * Mth.DEG_TO_RAD).xRot(tilt * Mth.DEG_TO_RAD).yRot(heading * Mth.DEG_TO_RAD);
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 0, false, 40, -10", "135, 0, 0, false, -120, 25", "30, 20, 0, false, 70, -30", "-60, 15, 10, true, 10, 20", "200, -25, -8, true, -150, -35"})
    void findsTheAimThatPointsAlongATarget(float heading, float roll, float tilt, boolean pitchFlipped, float yaw, float pitch)
    {
        SeatAimSolver.AimDirection mapping = driveable(heading, roll, tilt, pitchFlipped);
        Vec3 target = mapping.direction(yaw, pitch).scale(37D);

        SeatAimSolver.Aim aim = SeatAimSolver.solve(mapping, target, 0F, 0F);

        assertTrue(angleBetween(mapping.direction(aim.yaw(), aim.pitch()), target) < 0.1D, () -> "aim " + aim + " misses by " + angleBetween(mapping.direction(aim.yaw(), aim.pitch()), target));
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "45, 10", "-90, -20"})
    void anAimAlreadyOnTargetIsKept(float yaw, float pitch)
    {
        SeatAimSolver.AimDirection mapping = driveable(30F, 5F, 0F, false);
        SeatAimSolver.Aim aim = SeatAimSolver.solve(mapping, mapping.direction(yaw, pitch), yaw, pitch);
        assertEquals(yaw, aim.yaw(), 1.0E-3F);
        assertEquals(pitch, aim.pitch(), 1.0E-3F);
    }

    @ParameterizedTest
    @CsvSource({"0, 1, 0", "0.2, 0, 1"})
    void headingAndElevationFollowMinecraftConventions(double x, double y, double z)
    {
        Vec3 direction = new Vec3(x, y, z);
        float yaw = SeatAimSolver.heading(direction);
        float elevation = SeatAimSolver.elevation(direction);
        // directionFromRotation takes a pitch that is positive downwards, and its table-based trigonometry is good to about 0.01 degrees.
        Vec3 rebuilt = Vec3.directionFromRotation(-elevation, yaw);
        assertTrue(angleBetween(rebuilt, direction) < 0.02D);
    }
}
