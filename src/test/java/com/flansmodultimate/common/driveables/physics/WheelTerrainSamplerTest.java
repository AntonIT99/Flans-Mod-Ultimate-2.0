package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.api.driveables.WheelSupport;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.ToDoubleBiFunction;

import static org.junit.jupiter.api.Assertions.*;

class WheelTerrainSamplerTest
{
    /** A driveable's reach at step height 1 and spring 0.5: carried down to 0.45 below, tilted down to 1.9 below. */
    private static final WheelTerrainSampler.Reach REACH = new WheelTerrainSampler.Reach(1.6D, 2.35D, 0D, 1.1D, 1.9D, 0.45D);
    private static final float LIMIT = SuspensionPhysics.DEFAULT_TERRAIN_ANGLE;

    /** Terrain whose surface height is a function of where a probe comes down; NaN where there is none. */
    private static WheelTerrainSampler.Terrain terrain(ToDoubleBiFunction<Double, Double> height)
    {
        return (from, to) ->
        {
            double surface = height.applyAsDouble(from.x, from.z);
            if (Double.isNaN(surface) || surface > from.y || surface < to.y)
                return null;
            Vec3 point = new Vec3(from.x, surface, from.z);
            return new BlockHitResult(point, Direction.UP, BlockPos.containing(point.x, point.y - 0.5D, point.z), false);
        };
    }

    /** Four wheels two blocks ahead of and behind the middle, one to each side, the nose towards +X. */
    private static WheelTerrainSampler sample(WheelTerrainSampler.Terrain terrain, Vec3 prediction, boolean footprint)
    {
        WheelTerrainSampler sampler = new WheelTerrainSampler(terrain, REACH, prediction, footprint);
        for (int forward = -2; forward <= 2; forward += 4)
        {
            for (int right = -1; right <= 1; right += 2)
                sampler.add(new Vec3(forward, 64D, right), forward, right, 0D);
        }
        return sampler;
    }

    @Test
    void levelGroundCarriesEveryWheelWithoutTilt()
    {
        WheelTerrainSampler sampler = sample(terrain((x, z) -> 64.2D), Vec3.ZERO, true);
        assertEquals(4, sampler.grounded());
        assertEquals(0.2D, sampler.meanError(), 1.0E-9D);
        assertEquals(0.2D, sampler.highestError(), 1.0E-9D);
        assertEquals(0F, sampler.pitchTarget(false, LIMIT));
        assertEquals(0F, sampler.rollTarget(false, LIMIT));
        assertEquals(new WheelSupport(4, 4, Vec3.ZERO), sampler.support());
    }

    @Test
    void aSlopeTiltsTheVehicleAlongAndAcross()
    {
        WheelTerrainSampler sampler = sample(terrain((x, z) -> 64D + 0.1D * x - 0.05D * z), Vec3.ZERO, false);
        assertEquals(4, sampler.grounded());
        assertEquals((float) Math.toDegrees(Math.asin(0.4D / 4D)), sampler.pitchTarget(false, LIMIT), 1.0E-5F);
        assertEquals((float) -Math.toDegrees(Math.asin(0.1D / 2D)), sampler.rollTarget(false, LIMIT), 1.0E-5F);
        // A plane's pitch and roll turn the other way round.
        assertEquals(-sampler.pitchTarget(false, LIMIT), sampler.pitchTarget(true, LIMIT), 1.0E-6F);
    }

    @Test
    void theTiltKeepsToTheVehiclesOwnLimit()
    {
        // The rear a block lower than the front, over a wheelbase of a block and a half.
        WheelTerrainSampler.Terrain steep = terrain((x, z) -> x > 0D ? 64.5D : 63.5D);
        WheelTerrainSampler sampler = new WheelTerrainSampler(steep, REACH, Vec3.ZERO, false);
        sampler.add(new Vec3(0.75D, 64D, 0D), 0.75D, 0D, 0D);
        sampler.add(new Vec3(-0.75D, 64D, 0D), -0.75D, 0D, 0D);
        assertEquals(35F, sampler.pitchTarget(false, LIMIT));
        assertEquals((float) Math.toDegrees(Math.asin(1D / 1.5D)), sampler.pitchTarget(false, 50F), 1.0E-4F);
        assertEquals(40F, sampler.pitchTarget(false, 40F));
        assertEquals(35F, sampler.pitchTarget(false, Float.NaN));
    }

    @Test
    void theTiltLooksAheadButAWheelIsCarriedWhereItStands()
    {
        // Level ground that ends in a drop beyond x = 2.5: the front wheels stand on it and are heading over the edge.
        WheelTerrainSampler.Terrain ledge = terrain((x, z) -> x > 2.5D ? Double.NaN : 64D);
        Vec3 ahead = new Vec3(1D, 0D, 0D);
        WheelTerrainSampler sampler = sample(ledge, ahead, true);
        assertEquals(4, sampler.grounded());
        assertEquals(0D, sampler.meanError(), 1.0E-9D);
        assertEquals(new WheelSupport(4, 4, Vec3.ZERO), sampler.support());
        // Nothing ahead of the nose to pitch for: support at one end only eases level.
        assertEquals(0F, sampler.pitchTarget(false, LIMIT));
        // Once the front wheels stand beyond the edge themselves, nothing carries them.
        WheelTerrainSampler over = new WheelTerrainSampler(ledge, REACH, ahead, true);
        over.add(new Vec3(3D, 64D, 1D), 2D, 1D, 0D);
        over.add(new Vec3(3D, 64D, -1D), 2D, -1D, 0D);
        over.add(new Vec3(-1D, 64D, 1D), -2D, 1D, 0D);
        over.add(new Vec3(-1D, 64D, -1D), -2D, -1D, 0D);
        assertEquals(2, over.grounded());
        assertEquals(2, over.support().grounded());
        assertTrue(over.support().overhang().x > 0D);
    }

    @Test
    void aWheelHeadingIntoAWallIsStillCarriedByTheGroundItStandsOn()
    {
        // A wall two blocks high beyond x = 2.5 is more than the suspension compresses.
        WheelTerrainSampler.Terrain wall = terrain((x, z) -> x > 2.5D ? 66D : 64D);
        WheelTerrainSampler sampler = sample(wall, new Vec3(1D, 0D, 0D), false);
        assertEquals(4, sampler.grounded());
        assertEquals(0D, sampler.meanError(), 1.0E-9D);
    }

    @Test
    void aStepAheadLiftsAndPitchesTheVehicleBeforeItGetsThere()
    {
        // Half a block up beyond x = 2.5.
        WheelTerrainSampler.Terrain step = terrain((x, z) -> x > 2.5D ? 64.5D : 64D);
        WheelTerrainSampler sampler = sample(step, new Vec3(1D, 0D, 0D), false);
        assertEquals(4, sampler.grounded());
        assertEquals(0.25D, sampler.meanError(), 1.0E-9D);
        assertEquals(0.5D, sampler.highestError(), 1.0E-9D);
        assertTrue(sampler.pitchTarget(false, LIMIT) > 0F);
    }

    @Test
    void groundBelowTheSuspensionTravelTiltsButDoesNotCarry()
    {
        // The rear wheels hang a block above their ground: within the pose probe, beyond the droop that carries.
        WheelTerrainSampler sampler = sample(terrain((x, z) -> x > 0D ? 64D : 63D), Vec3.ZERO, true);
        assertEquals(2, sampler.grounded());
        assertEquals(0D, sampler.meanError(), 1.0E-9D);
        assertTrue(sampler.pitchTarget(false, LIMIT) > 0F);
        assertEquals(2, sampler.support().grounded());
        assertTrue(sampler.support().overhang().x < 0D);
    }

    @Test
    void nothingUnderTheWheelsCarriesNothing()
    {
        WheelTerrainSampler sampler = sample(terrain((x, z) -> Double.NaN), Vec3.ZERO, true);
        assertEquals(0, sampler.grounded());
        assertEquals(0D, sampler.meanError());
        assertEquals(Double.NEGATIVE_INFINITY, sampler.highestError());
        assertTrue(sampler.support().unsupported());
        WheelTerrainSampler untracked = sample(terrain((x, z) -> 64D), Vec3.ZERO, false);
        assertThrows(IllegalStateException.class, untracked::support);
    }
}
