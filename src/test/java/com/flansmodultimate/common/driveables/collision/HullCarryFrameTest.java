package com.flansmodultimate.common.driveables.collision;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HullCarryFrameTest
{
    /** SdKfz 251 Ausf. D's core: SetupPart core 1000 -55 -6 -21 108 28 42, in model pixels. */
    private static DriveableHullGeometry halfTrack()
    {
        CollisionBox box = CollisionBox.inWorldUnits(1000F, -55F / 16F, -6F / 16F, -21F / 16F, 108F / 16F, 28F / 16F, 42F / 16F, 0F, 0F);
        return new DriveableHullGeometry(PartBodyCollisionHelper.profile(Map.of(EnumDriveablePart.CORE, box), 1F));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void walkingOnATranslatingSdKfzDoesNotInventAnyTurnOrDrop(boolean npc)
    {
        checkMotion(npc, 0F);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void walkingOnATurningSdKfzFollowsOnlyItsActualTurn(boolean npc)
    {
        // The definition's 18 degrees/second is 0.9 degrees/tick.
        checkMotion(npc, 0.9F);
    }

    @Test
    void aMeshWithACollapsedCornerKeepsItsAlternativeFrameThroughPitchAndRoll()
    {
        checkMeshCarry(DriveableCollisionProfile.of(List.of(mesh(1F, true))));
    }

    @Test
    void aFlatMeshCanStillBorrowTheRigidFrameOfASolidHullPart()
    {
        CollisionBox solid = CollisionBox.inWorldUnits(100F, -1F, 0F, -2F, 2F, 1F, 4F, 0F, 0F);
        checkMeshCarry(DriveableCollisionProfile.of(List.of(mesh(0F, false), DriveableCollisionProfile.compilePartBox(EnumDriveablePart.RIGHT_SKIRT, solid))));
    }

    private static void checkMotion(boolean npc, float turn)
    {
        DriveableHullGeometry geometry = halfTrack();
        Vec3 origin = new Vec3(123.456D, 64D, -456.789D);
        float yaw = 37.6F;
        pose(geometry, npc, origin, yaw, false);
        double[] carried = new double[3];
        for (int tick = 1; tick <= 200; tick++)
        {
            // Walk across the top face between vehicle updates, away from the box's symmetry centre.
            double across = 0.2D + tick * 0.001D;
            double along = 0.7D - tick * 0.001D;
            Vec3 feet = deckPoint(geometry, across, along);
            origin = origin.add(0.12D, 0D, 0.07D);
            float previousYaw = yaw;
            yaw += turn;
            pose(geometry, npc, origin, yaw, true);
            int support = geometry.findSupport(feet.x - 0.3D, feet.y, feet.z - 0.3D, feet.x + 0.3D, feet.y + 1.8D, feet.z + 0.3D);
            assertEquals(0, support, "Lost support at tick " + tick);
            assertTrue(geometry.carryPoint(support, feet.x, feet.y, feet.z, carried));
            Vec3 expected = deckPoint(geometry, across, along);
            assertEquals(expected.x, carried[0], 1.0E-7D, "Unexpected sideways displacement at tick " + tick);
            assertEquals(expected.y, carried[1], 1.0E-7D, "Unexpected drop at tick " + tick);
            assertEquals(expected.z, carried[2], 1.0E-7D, "Unexpected forward displacement at tick " + tick);
            assertEquals(yaw - previousYaw, geometry.carryYaw(support), 1.0E-4F, "Unexpected camera turn at tick " + tick);
        }
    }

    /** Uses the same physical corners in both poses, independently of the frame selected internally for carrying. */
    private static Vec3 deckPoint(DriveableHullGeometry geometry, double across, double along)
    {
        double[] vertices = geometry.copyCurrentVertices(0);
        assertNotNull(vertices);
        Vec3 corner = new Vec3(vertices[0], vertices[1], vertices[2]);
        Vec3 width = new Vec3(vertices[3], vertices[4], vertices[5]).subtract(corner);
        Vec3 length = new Vec3(vertices[9], vertices[10], vertices[11]).subtract(corner);
        return corner.add(width.scale(across)).add(length.scale(along));
    }

    private static void pose(DriveableHullGeometry geometry, boolean npc, Vec3 origin, float yaw, boolean continuous)
    {
        if (npc)
            PartBodyCollisionHelper.pose(geometry, origin, yaw, 0F, 0F, 1F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> true, continuous);
        else
            geometry.update(origin.x, origin.y, origin.z, yaw, 0F, 0F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> true, continuous);
    }

    private static DriveableCollisionProfile.Shape mesh(float height, boolean collapseCorner)
    {
        List<Vector3f> modifiers = new ArrayList<>();
        for (int corner = 0; corner < 8; corner++)
            modifiers.add(new Vector3f());
        if (collapseCorner)
            modifiers.set(0, new Vector3f(-4F, 0F, 0F));
        DriveableCollisionProfile.Shape shape = DriveableCollisionProfile.compileMesh(new CollisionMesh(new Vector3f(), new Vector3f(4F, height, 3F), modifiers, EnumDriveablePart.CORE));
        assertNotNull(shape);
        return shape;
    }

    private static void checkMeshCarry(DriveableCollisionProfile profile)
    {
        DriveableHullGeometry geometry = new DriveableHullGeometry(profile);
        double[] carried = new double[3];
        for (int tick = 0; tick <= 100; tick++)
        {
            double[] previous = geometry.copyCurrentVertices(0);
            geometry.update(123.456D + tick * 0.12D, 64D, -456.789D + tick * 0.07D, tick * 1.3F, tick * 0.23F, tick * -0.17F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> true, tick > 0);
            if (previous == null)
                continue;
            double[] current = geometry.copyCurrentVertices(0);
            assertNotNull(current);
            assertTrue(geometry.carryPoint(0, previous[21], previous[22], previous[23], carried));
            assertEquals(current[21], carried[0], 1.0E-7D);
            assertEquals(current[22], carried[1], 1.0E-7D);
            assertEquals(current[23], carried[2], 1.0E-7D);
        }
    }
}
