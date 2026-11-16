package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcDriving.Settings;
import com.wolffsmod.npcs.sim.NpcDriving.Situation;
import com.wolffsmod.npcs.sim.NpcVehiclePaths.Solids;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class NpcLedgesTest
{
    private static final Settings TRACKED = new Settings(true, 4.5F, 3F, 0.05F, 0.125F, 0.03F, 0.4F, 0.45F, 0.15F, true, 110F, 10D);
    private static final Vec3 EAST = new Vec3(1D, 0D, 0D);
    /** Standing in the middle of the block at x = 0, on ground whose surface is at 64. */
    private static final Vec3 ORIGIN = new Vec3(0.5D, 64D, 0.5D);
    private static final int VOID = Integer.MIN_VALUE;

    /** Ground along the x axis: solid up to the height of each column, and nothing where it is {@link #VOID}. */
    private static Solids ground(IntUnaryOperator top)
    {
        return (x, y, z) -> top.applyAsInt(x) != VOID && y <= top.applyAsInt(x);
    }

    private static double ahead(IntUnaryOperator top, double reach, int step)
    {
        return NpcLedges.distance(ground(top), ORIGIN, EAST, reach, step, step);
    }

    @Test
    void levelGroundAndStepsTheVehicleTakesAreNoLedge()
    {
        assertEquals(NpcLedges.NONE, ahead(x -> 63, 8D, 1));
        // One block down at a time, as far as it goes.
        assertEquals(NpcLedges.NONE, ahead(x -> 63 - x, 8D, 1));
        assertEquals(NpcLedges.NONE, ahead(x -> 63 + x, 8D, 1));
    }

    @Test
    void aDropDeeperThanTheVehicleDrivesDownIsFoundWhereItBegins()
    {
        assertEquals(4D, ahead(x -> x >= 4 ? 60 : 63, 8D, 1));
        assertEquals(4D, ahead(x -> x >= 4 ? VOID : 63, 8D, 1));
        // Two blocks down is a drop to a vehicle that climbs one, and a step to one that climbs two.
        assertEquals(3D, ahead(x -> x >= 3 ? 61 : 63, 8D, 1));
        assertEquals(NpcLedges.NONE, ahead(x -> x >= 3 ? 61 : 63, 8D, 2));
        // Beyond its reach, it is not looked for.
        assertEquals(NpcLedges.NONE, ahead(x -> x >= 4 ? 60 : 63, 3D, 1));
    }

    @Test
    void aDropBehindAStepDownIsMeasuredFromTheStep()
    {
        // One block down at x = 2, which it takes, then three more at x = 4.
        assertEquals(4D, ahead(x -> x >= 4 ? 59 : x >= 2 ? 62 : 63, 8D, 1));
    }

    @Test
    void aWallEndsTheLook()
    {
        // The pit behind the wall is not where it is driving.
        assertEquals(NpcLedges.NONE, ahead(x -> x == 2 ? 66 : x > 2 ? VOID : 63, 8D, 1));
    }

    @Test
    void aWallOrLedgeThatDropsAwayBehindItIsADropWhereItBegins()
    {
        // A wall one block high and one wide at x = 2, with the ground three blocks lower beyond it.
        IntUnaryOperator wall = x -> x == 2 ? 64 : x > 2 ? 61 : 63;
        assertEquals(2D, ahead(wall, 8D, 1));
        // Following a path, which priced the climb, only the drop behind it counts.
        assertEquals(3D, NpcLedges.distance(ground(wall), ORIGIN, EAST, 8D, 1, 1, false));
        // Two blocks wide it is still a ledge; three blocks wide the hull stands on it.
        assertEquals(2D, ahead(x -> x == 2 || x == 3 ? 64 : x > 3 ? 61 : 63, 8D, 1));
        assertEquals(5D, ahead(x -> x >= 2 && x <= 4 ? 64 : x > 4 ? 61 : 63, 8D, 1));
        // A curb with the ground back at its own level behind it is driven over.
        assertEquals(NpcLedges.NONE, ahead(x -> x == 2 ? 64 : 63, 8D, 1));
        // So is a step up onto ground that drops only as far as the vehicle drives down.
        assertEquals(NpcLedges.NONE, ahead(x -> x == 2 ? 65 : x > 2 ? 63 : 63, 8D, 2));
        // A wall behind the step ends the look before the ground behind it is seen.
        assertEquals(NpcLedges.NONE, ahead(x -> x == 2 ? 64 : x == 3 ? 67 : x > 3 ? VOID : 63, 8D, 1));
    }

    @Test
    void theLookFollowsTheHeading()
    {
        Solids pitToTheNorth = (x, y, z) -> z >= 0 && y <= 63;
        assertEquals(1D, NpcLedges.distance(pitToTheNorth, ORIGIN, new Vec3(0D, 0D, -1D), 8D, 1, 1));
        assertEquals(NpcLedges.NONE, NpcLedges.distance(pitToTheNorth, ORIGIN, new Vec3(0D, 0D, 1D), 8D, 1, 1));
        // Diagonally it is the column across the corner that is looked at.
        assertEquals(1D, NpcLedges.distance(pitToTheNorth, ORIGIN, new Vec3(Math.sqrt(0.5D), 0D, -Math.sqrt(0.5D)), 8D, 1, 1));
    }

    @Test
    void aVehicleBrakesToAHaltShortOfALedge()
    {
        NpcDriving driving = new NpcDriving();
        double topSpeed = 0.45D;
        double remaining = 12D;
        for (int tick = 0; tick < 400 && remaining > 0D; tick++)
            remaining -= topSpeed * driving.drive(new Situation(0F, 0F, 100D, 0F, Double.MAX_VALUE, 4D, topSpeed, 1F, 0D, topSpeed, remaining), TRACKED).throttle();
        // It comes to rest at the distance it was given, which the caller keeps short of the edge by a margin wider than
        // the little a throttle shed in whole steps rolls on.
        assertTrue(remaining > -0.25D);
        assertTrue(remaining < 1D);
        float throttle = 1F;
        for (int tick = 0; tick < 10; tick++)
            throttle = driving.drive(new Situation(0F, 0F, 100D, 0F, Double.MAX_VALUE, 4D, 0D, 1F, 0D, topSpeed, remaining), TRACKED).throttle();
        assertTrue(throttle < 0.1F);
    }

    @Test
    void atTheEdgeItStandsAndWithoutALedgeItDrivesOn()
    {
        NpcDriving held = new NpcDriving();
        NpcDriving free = new NpcDriving();
        for (int tick = 0; tick < 40; tick++)
        {
            held.drive(new Situation(0F, 0F, 100D, 0F, Double.MAX_VALUE, 4D, 0D, 1F, 0D, 0.45D, 0D), TRACKED);
            free.drive(new Situation(0F, 0F, 100D, 0F, Double.MAX_VALUE, 4D, 0D, 1F, 0D, 0.45D), TRACKED);
        }
        assertEquals(0F, held.drive(new Situation(0F, 0F, 100D, 0F, Double.MAX_VALUE, 4D, 0D, 1F, 0D, 0.45D, -0.3D), TRACKED).throttle());
        assertEquals(1F, free.drive(new Situation(0F, 0F, 100D, 0F, Double.MAX_VALUE, 4D, 0D, 1F, 0D, 0.45D), TRACKED).throttle(), 1.0E-6F);
    }
}
