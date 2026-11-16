package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.GroundPropulsion;
import com.wolffsmod.npcs.sim.NpcVehiclePaths.Profile;
import com.wolffsmod.npcs.sim.NpcVehiclePaths.Solids;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.OptionalDouble;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

class NpcVehiclePathsTest
{
    /** A level of nothing but the blocks put into it. */
    private static final class Blocks implements Solids
    {
        private final Set<Long> solid = new HashSet<>();

        Blocks put(int x, int y, int z)
        {
            solid.add(BlockPos.asLong(x, y, z));
            return this;
        }

        /** Level ground with its surface below the given feet level, along the x axis. */
        Blocks ground(int fromX, int toX, int feet)
        {
            for (int x = fromX; x <= toX; x++)
                put(x, feet - 1, 0);
            return this;
        }

        @Override
        public boolean solid(int x, int y, int z)
        {
            return solid.contains(BlockPos.asLong(x, y, z));
        }
    }

    private static Profile tank(double step)
    {
        return NpcVehiclePaths.profile(1.5D, 2.4D, OptionalDouble.of(step), null, null);
    }

    @Test
    void aHullNeedsRoomOnBothSidesOfTheNpc()
    {
        Profile tank = tank(1D);
        // A walker one block wide fits a corridor one block wide; a hull three blocks across does not.
        assertFalse(tank.blocked(0, 64, 0, 1, new Blocks()));
        assertTrue(tank.blocked(0, 64, 0, 1, new Blocks().put(-1, 65, 0)));
        assertTrue(tank.blocked(0, 64, 0, 1, new Blocks().put(1, 65, 0)));
        assertTrue(tank.blocked(0, 64, 0, 1, new Blocks().put(0, 65, -1)));
        assertTrue(tank.blocked(0, 64, 0, 1, new Blocks().put(1, 66, 1)));
        // Two blocks from the middle, the wall is clear of the hull.
        assertFalse(tank.blocked(0, 64, 0, 1, new Blocks().put(2, 65, 0).put(-2, 65, 0)));
        // A roof above the hull does not stop it.
        assertFalse(tank.blocked(0, 64, 0, 1, new Blocks().put(1, 67, 0)));
    }

    @Test
    void whatTheVehicleDrivesOverDoesNotBlockItsHull()
    {
        // The ground a step up ahead lies inside the hull's footprint, and so does a curb beside it.
        assertFalse(tank(1D).blocked(0, 64, 0, 1, new Blocks().put(1, 64, 0).put(-1, 64, 1)));
        assertTrue(tank(1D).blocked(0, 64, 0, 1, new Blocks().put(1, 64, 0).put(1, 65, 0)));
        // One that climbs two blocks drives over a wall of two.
        assertFalse(tank(2D).blocked(0, 64, 0, 1, new Blocks().put(1, 64, 0).put(1, 65, 0)));
        assertTrue(tank(2D).blocked(0, 64, 0, 1, new Blocks().put(1, 66, 0)));
    }

    @Test
    void aWiderHitboxIsCheckedAroundItsOwnMiddle()
    {
        // A hitbox two blocks wide stands on the node and the one east and south of it, its middle between them.
        Profile tank = tank(1D);
        assertTrue(tank.blocked(0, 64, 0, 2, new Blocks().put(-1, 65, 0)));
        assertTrue(tank.blocked(0, 64, 0, 2, new Blocks().put(2, 65, 1)));
        assertFalse(tank.blocked(0, 64, 0, 2, new Blocks().put(3, 65, 0).put(-2, 65, 0)));
        // The search itself answers for the blocks of the hitbox.
        assertFalse(tank.blocked(0, 64, 0, 2, new Blocks().put(1, 65, 1)));
    }

    @Test
    void theProfileKeepsToTheBoundsSearched()
    {
        Profile huge = NpcVehiclePaths.profile(9D, 12D, OptionalDouble.of(2.5D), null, null);
        assertEquals(NpcVehiclePaths.MAX_HALF_WIDTH, huge.halfWidth());
        assertEquals(NpcVehiclePaths.MAX_HEIGHT, huge.height());
        assertEquals(2, huge.climbBlocks());
        assertEquals(4, huge.startAllowance());
        // Something above the height searched is still checked where the vehicle climbs that high.
        assertTrue(NpcVehiclePaths.profile(1.5D, 0.5D, OptionalDouble.of(1D), null, null).blocked(0, 64, 0, 1, new Blocks().put(1, 65, 0)));
    }

    @Test
    void aStepAboveTheStepHeightIsRejected()
    {
        Profile tank = tank(1D);
        assertTrue(tank.climbs(0D));
        assertTrue(tank.climbs(-3D));
        assertTrue(tank.climbs(0.5D));
        assertTrue(tank.climbs(1D));
        // A carpet on the block ahead is more than a step height of one block drives up, though a mob jumps it.
        assertFalse(tank.climbs(1.0625D));
        assertFalse(tank.climbs(2D));
        assertTrue(tank(2.5D).climbs(2D));
        assertFalse(tank(2.5D).climbs(3D));
    }

    @Test
    void aVehicleThatJumpsLikeAMobIsHeldToNoStep()
    {
        Profile jumper = NpcVehiclePaths.profile(1.5D, 2.4D, OptionalDouble.empty(), null, null);
        assertTrue(jumper.climbs(1.125D));
        assertEquals(1, jumper.climbBlocks());
    }

    @Test
    void aSingleStepIsAGentlerGradeThanAStaircase()
    {
        // Stepped up onto x = 1 at feet 65, the ground keeps level for as far as it is followed.
        double curb = tank(1D).grade(new Blocks().ground(1, 4, 65), 1, 65, 0, 1, 0, 1D);
        assertEquals(Math.toDegrees(Math.atan2(1D, 4D)), curb, 1.0E-9D);
        // Every block ahead a block higher.
        Blocks stairs = new Blocks().put(1, 64, 0).put(2, 65, 0).put(3, 66, 0).put(4, 67, 0);
        assertEquals(45D, tank(1D).grade(stairs, 1, 65, 0, 1, 0, 1D), 1.0E-9D);
        // A rise every other block.
        Blocks hill = new Blocks().put(1, 64, 0).put(2, 64, 0).put(3, 65, 0).put(4, 65, 0);
        assertEquals(Math.toDegrees(Math.atan2(2D, 4D)), tank(1D).grade(hill, 1, 65, 0, 1, 0, 1D), 1.0E-9D);
        // Diagonally the same rise is spread over a longer run.
        Blocks diagonal = new Blocks();
        for (int step = 1; step <= 4; step++)
            diagonal.put(step, 64, step);
        assertEquals(Math.toDegrees(Math.atan2(1D, 4D * Math.sqrt(2D))), tank(1D).grade(diagonal, 1, 65, 1, 1, 1, 1D), 1.0E-9D);
    }

    @Test
    void aSlopeEndsAtAWallOrADrop()
    {
        // Onto a ridge one block deep: the ground drops away behind it.
        assertEquals(45D, tank(1D).grade(new Blocks().put(1, 64, 0), 1, 65, 0, 1, 0, 1D), 1.0E-9D);
        // A wall two blocks high after one level block is no part of the slope.
        Blocks wall = new Blocks().ground(1, 2, 65).put(3, 65, 0).put(3, 66, 0);
        assertEquals(Math.toDegrees(Math.atan2(1D, 2D)), tank(1D).grade(wall, 1, 65, 0, 1, 0, 1D), 1.0E-9D);
        // Level and downhill moves have no grade.
        assertEquals(0D, tank(1D).grade(new Blocks().ground(1, 4, 65), 1, 65, 0, 1, 0, 0D));
        assertEquals(0D, tank(1D).grade(new Blocks().ground(1, 4, 65), 1, 65, 0, 1, 0, -1D));
    }

    @Test
    void aStepOntoAWallOrLedgeIsARidgeAndOneOntoASlopeIsNot()
    {
        // Stepped up onto x = 1 at feet 65, heading +X: a wall one block wide, and a ledge two blocks wide.
        assertTrue(NpcVehiclePaths.ridge(new Blocks().put(1, 64, 0), 1, 65, 0, 1, 0));
        assertTrue(NpcVehiclePaths.ridge(new Blocks().ground(1, 2, 65), 1, 65, 0, 1, 0));
        // Ground carrying on as far as the ridge look reaches is a slope or a terrace.
        assertFalse(NpcVehiclePaths.ridge(new Blocks().ground(1, 3, 65), 1, 65, 0, 1, 0));
        // Higher ground ahead is still ground under the far side.
        assertFalse(NpcVehiclePaths.ridge(new Blocks().put(1, 64, 0).put(2, 64, 0).put(2, 65, 0).put(3, 64, 0), 1, 65, 0, 1, 0));
        // Only the direction travelled in is looked along; without one there is nothing to look along.
        assertFalse(NpcVehiclePaths.ridge(new Blocks().put(1, 64, 0), 1, 65, 0, 0, 0));
        assertTrue(NpcVehiclePaths.ridge(new Blocks().put(1, 64, 0).put(2, 64, 0).put(3, 64, 0), 1, 65, 0, 0, 1));
    }

    @Test
    void aGradeCostsWhatItSlowsTheVehicleDownBy()
    {
        GroundPropulsion engine = mock(GroundPropulsion.class);
        // Full speed up to 10 degrees, half at 20, a crawl at 40 and nothing beyond.
        when(engine.climbFactor(anyDouble())).thenAnswer(call ->
        {
            double degrees = call.getArgument(0);
            if (degrees <= 10D)
                return 1D;
            if (degrees <= 20D)
                return 0.5D;
            return degrees <= 40D ? 0.25D : 0D;
        });
        Profile tank = NpcVehiclePaths.profile(1.5D, 2.4D, OptionalDouble.of(1D), engine, null);
        assertEquals(0F, tank.gradeCost(0D));
        assertEquals(0F, tank.gradeCost(8D));
        assertEquals(1F, tank.gradeCost(20D), 1.0E-6F);
        assertEquals(3F, tank.gradeCost(40D), 1.0E-6F);
        // A grade the vehicle cannot drive stays a last resort rather than a wall.
        assertEquals(NpcVehiclePaths.MAX_GRADE_COST, tank.gradeCost(45D));
        // Without slope physics, or with a legacy response, a grade slows nothing down.
        assertEquals(0F, tank(1D).gradeCost(45D));
    }

    @Test
    void softGroundIsDearerThanFirmGround()
    {
        assertEquals(0F, NpcVehiclePaths.surfaceCost(1F));
        assertEquals(1F, NpcVehiclePaths.surfaceCost(0.5F), 1.0E-6F);
        assertTrue(NpcVehiclePaths.surfaceCost(0.6F) < NpcVehiclePaths.surfaceCost(0.4F));
        assertTrue(NpcVehiclePaths.surfaceCost(0.85F) > 0F);
        assertEquals(NpcVehiclePaths.MAX_SURFACE_COST, NpcVehiclePaths.surfaceCost(0.01F));
        // Four soft blocks in a row are worth a firm detour of more than four blocks, one is not worth a long one.
        assertTrue(4F * NpcVehiclePaths.surfaceCost(0.5F) >= 4F);
        assertTrue(NpcVehiclePaths.surfaceCost(0.85F) < 1F);
    }
}
