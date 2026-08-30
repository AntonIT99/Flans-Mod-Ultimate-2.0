package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriveableCollisionProfileTest
{
    private static final double EPSILON = 1.0E-6D;

    @Test
    void expandsLegacyMeshCoordinatesAndInvertsModelY()
    {
        CollisionMesh mesh = mesh(new Vector3f(1F, 2F, 3F), new Vector3f(4F, 5F, 6F), zeroModifiers());
        DriveableCollisionProfile.Shape shape = DriveableCollisionProfile.compileMesh(mesh);

        assertNotNull(shape);
        assertVertex(shape, 0, 3D, -2D, -1D);
        assertVertex(shape, 2, 9D, -2D, -5D);
        assertVertex(shape, 4, 3D, -7D, -1D);
        assertVertex(shape, 6, 9D, -7D, -5D);
        assertTrue(shape.isConvex());
    }

    @Test
    void appliesEachCornerModifierWithLegacyExpansionSigns()
    {
        List<Vector3f> modifiers = zeroModifiers();
        modifiers.set(0, new Vector3f(0.25F, 0.5F, 0.75F));
        modifiers.set(6, new Vector3f(1F, 1.25F, 1.5F));
        DriveableCollisionProfile.Shape shape = DriveableCollisionProfile.compileMesh(
            mesh(new Vector3f(2F, 3F, 4F), new Vector3f(5F, 6F, 7F), modifiers));

        assertNotNull(shape);
        assertVertex(shape, 0, 3.25D, -2.5D, -1.75D);
        assertVertex(shape, 6, 12.5D, -10.25D, -8D);
    }

    @Test
    void compiledFacePlanesPointOutwardAndContainAllCorners()
    {
        DriveableCollisionProfile.Shape shape = DriveableCollisionProfile.compileMesh(
            mesh(new Vector3f(), new Vector3f(2F, 1F, 3F), zeroModifiers()));
        assertNotNull(shape);
        double[] points = shape.coordinates();
        double centreX = average(points, 0);
        double centreY = average(points, 1);
        double centreZ = average(points, 2);
        double[] plane = new double[4];

        for (int[] face : DriveableCollisionProfile.FACE_QUADS)
        {
            assertTrue(DriveableCollisionProfile.facePlane(points, face, centreX, centreY, centreZ, plane));
            for (int vertex = 0; vertex < 8; vertex++)
            {
                double signed = plane[0] * shape.vertex(vertex, 0) + plane[1] * shape.vertex(vertex, 1)
                    + plane[2] * shape.vertex(vertex, 2) + plane[3];
                assertTrue(signed <= EPSILON, "corner must remain inside every outward face plane");
            }
        }
    }

    @Test
    void projectedBarycentricCoordinatesAcceptDeckInteriorAndRejectOutside()
    {
        DriveableCollisionProfile.Shape shape = DriveableCollisionProfile.compileMesh(
            mesh(new Vector3f(), new Vector3f(4F, 1F, 4F), zeroModifiers()));
        assertNotNull(shape);
        double[] weights = new double[3];
        int[] triangle = DriveableCollisionProfile.TOP_TRIANGLES[0];

        assertTrue(DriveableCollisionHelper.barycentricXZ(shape.coordinates(), triangle, 1D, -3D, weights));
        assertEquals(1D, weights[0] + weights[1] + weights[2], EPSILON);
        assertFalse(DriveableCollisionHelper.barycentricXZ(shape.coordinates(), triangle, 1D, 1D, weights));

    }

    @Test
    void stationaryDeckCorrectsGravityPenetration()
    {
        assertEquals(0.0801D, DriveableCollisionHelper.supportVerticalCorrection(4D, 3.92D), EPSILON);
        assertEquals(-0.0199D, DriveableCollisionHelper.supportVerticalCorrection(4D, 4.02D), EPSILON);
    }

    @Test
    void deckSupportWinsAcrossNormalPlayerStepDepth()
    {
        assertTrue(DriveableCollisionHelper.isSupportContact(-0.6D, -0.08D, false));
        assertTrue(DriveableCollisionHelper.isSupportContact(0.2D, 0D, true));
        assertFalse(DriveableCollisionHelper.isSupportContact(-0.8D, -0.08D, false));
        assertFalse(DriveableCollisionHelper.isSupportContact(0D, 0.6D, false));
    }

    @Test
    void landingToleranceSweepsFastFallingEntitiesOntoDecks()
    {
        assertEquals(0.3D, DriveableCollisionHelper.sweptLandingTolerance(-0.08D), EPSILON);
        assertEquals(0.9D, DriveableCollisionHelper.sweptLandingTolerance(-0.8D), EPSILON);
        assertEquals(1.5D, DriveableCollisionHelper.sweptLandingTolerance(-3D), EPSILON);
    }

    @Test
    void rotatesLegacyPartBoxesIntoTheModernDriveableBasis()
    {
        CollisionBox box = new CollisionBox(100F, 16F, 32F, 48F, 64F, 80F, 96F);

        assertEquals(3D, box.getX(), EPSILON);
        assertEquals(2D, box.getY(), EPSILON);
        assertEquals(-5D, box.getZ(), EPSILON);
        assertEquals(6D, box.getWidth(), EPSILON);
        assertEquals(5D, box.getHeight(), EPSILON);
        assertEquals(4D, box.getDepth(), EPSILON);
    }

    @Test
    void convertsLegacyPointsAndDirectionsConsistently()
    {
        var converted = LegacyDriveableCoordinates.toLocal(new Vector3f(2F, 3F, 5F));

        assertEquals(5D, converted.x, EPSILON);
        assertEquals(3D, converted.y, EPSILON);
        assertEquals(-2D, converted.z, EPSILON);
    }

    private static CollisionMesh mesh(Vector3f position, Vector3f size, List<Vector3f> modifiers)
    {
        return new CollisionMesh(position, size, modifiers, EnumDriveablePart.CORE);
    }

    private static List<Vector3f> zeroModifiers()
    {
        List<Vector3f> result = new ArrayList<>(8);
        for (int index = 0; index < 8; index++)
            result.add(new Vector3f());
        return result;
    }

    private static void assertVertex(DriveableCollisionProfile.Shape shape, int vertex,
                                     double x, double y, double z)
    {
        assertEquals(x, shape.vertex(vertex, 0), EPSILON);
        assertEquals(y, shape.vertex(vertex, 1), EPSILON);
        assertEquals(z, shape.vertex(vertex, 2), EPSILON);
    }

    private static double average(double[] points, int axis)
    {
        double result = 0D;
        for (int vertex = 0; vertex < 8; vertex++)
            result += points[vertex * 3 + axis];
        return result / 8D;
    }
}
