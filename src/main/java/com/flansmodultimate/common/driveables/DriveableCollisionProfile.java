package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.types.DriveableType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Immutable, precomputed geometry used for driveable-to-entity collision.
 *
 * <p>The legacy {@code AddCollisionMesh} points are corner modifiers rather
 * than absolute vertices. They are expanded here once when the type is first
 * used, instead of rebuilding and mutating collision boxes for every entity
 * on every tick. Damage and projectile tracing deliberately continue to use
 * the exact per-part {@link CollisionBox} definitions.</p>
 */
public final class DriveableCollisionProfile
{
    /** Two triangles per face, in the legacy shaped-box corner order. */
    static final int[][] TOP_TRIANGLES = {{2, 1, 0}, {3, 2, 0}};
    static final int[][] FACE_QUADS = {
        {0, 1, 2, 3}, // top
        {4, 7, 6, 5}, // bottom
        {1, 5, 6, 2}, // +x
        {3, 7, 4, 0}, // -x
        {0, 4, 5, 1}, // -z
        {2, 6, 7, 3}  // +z
    };

    private final List<Shape> shapes;
    private final boolean usingFancyMeshes;

    private DriveableCollisionProfile(List<Shape> shapes, boolean usingFancyMeshes)
    {
        this.shapes = List.copyOf(shapes);
        this.usingFancyMeshes = usingFancyMeshes;
    }

    public static DriveableCollisionProfile compile(DriveableType type)
    {
        if (type == null)
            return new DriveableCollisionProfile(List.of(), false);

        List<CollisionMesh> meshes = type.getCollisionMeshes();
        boolean useMeshes = type.isFancyCollision() && meshes != null && !meshes.isEmpty();
        List<Shape> result = new ArrayList<>();
        if (useMeshes)
        {
            for (CollisionMesh mesh : meshes)
            {
                Shape shape = compileMesh(mesh);
                if (shape != null)
                    result.add(shape);
            }
        }
        else
        {
            for (Map.Entry<EnumDriveablePart, CollisionBox> entry : type.getHealth().entrySet())
            {
                Shape shape = compilePartBox(entry.getKey(), entry.getValue());
                if (shape != null)
                    result.add(shape);
            }
        }
        return new DriveableCollisionProfile(result, useMeshes);
    }

    /** Package-visible for focused geometry tests. */
    static Shape compileMesh(CollisionMesh mesh)
    {
        if (mesh == null || mesh.position() == null || mesh.size() == null
            || mesh.vertices() == null || mesh.vertices().size() != 8)
            return null;

        Vector3f pos = mesh.position();
        Vector3f size = mesh.size();
        List<Vector3f> modifier = mesh.vertices();
        double[] points = new double[24];

        // These equations intentionally preserve CollisionShapeBox from
        // 1.7.10. The old constructor's -10/16 Y shift was cancelled again in
        // checkCollision, leaving the model-Y inversion represented below.
        set(points, 0, pos.x - modifier.get(0).x, -pos.y + modifier.get(0).y, pos.z - modifier.get(0).z);
        set(points, 1, pos.x + size.x + modifier.get(1).x, -pos.y + modifier.get(1).y, pos.z - modifier.get(1).z);
        set(points, 2, pos.x + size.x + modifier.get(2).x, -pos.y + modifier.get(2).y,
            pos.z + size.z + modifier.get(2).z);
        set(points, 3, pos.x - modifier.get(3).x, -pos.y + modifier.get(3).y,
            pos.z + size.z + modifier.get(3).z);
        set(points, 4, pos.x - modifier.get(4).x, -pos.y - size.y - modifier.get(4).y,
            pos.z - modifier.get(4).z);
        set(points, 5, pos.x + size.x + modifier.get(5).x, -pos.y - size.y - modifier.get(5).y,
            pos.z - modifier.get(5).z);
        set(points, 6, pos.x + size.x + modifier.get(6).x, -pos.y - size.y - modifier.get(6).y,
            pos.z + size.z + modifier.get(6).z);
        set(points, 7, pos.x - modifier.get(7).x, -pos.y - size.y - modifier.get(7).y,
            pos.z + size.z + modifier.get(7).z);

        LegacyDriveableCoordinates.toLocalVertices(points);

        return allFinite(points) ? new Shape(mesh.part(), points) : null;
    }

    private static Shape compilePartBox(EnumDriveablePart part, CollisionBox box)
    {
        if (box == null || box.getWidth() <= 0F || box.getHeight() <= 0F || box.getDepth() <= 0F)
            return null;
        double x0 = box.getX();
        double y0 = box.getY();
        double z0 = box.getZ();
        double x1 = x0 + box.getWidth();
        double y1 = y0 + box.getHeight();
        double z1 = z0 + box.getDepth();
        double[] points = new double[24];
        set(points, 0, x0, y1, z0);
        set(points, 1, x1, y1, z0);
        set(points, 2, x1, y1, z1);
        set(points, 3, x0, y1, z1);
        set(points, 4, x0, y0, z0);
        set(points, 5, x1, y0, z0);
        set(points, 6, x1, y0, z1);
        set(points, 7, x0, y0, z1);
        return allFinite(points) ? new Shape(part, points) : null;
    }

    private static void set(double[] points, int vertex, double x, double y, double z)
    {
        int offset = vertex * 3;
        points[offset] = x;
        points[offset + 1] = y;
        points[offset + 2] = z;
    }

    private static boolean allFinite(double[] values)
    {
        for (double value : values)
        {
            if (!Double.isFinite(value))
                return false;
        }
        return true;
    }

    public List<Shape> getShapes()
    {
        return shapes;
    }

    public boolean isUsingFancyMeshes()
    {
        return usingFancyMeshes;
    }

    public boolean isEmpty()
    {
        return shapes.isEmpty();
    }

    /** One convex shaped box associated with a destructible driveable part. */
    public static final class Shape
    {
        private final EnumDriveablePart part;
        private final boolean turret;
        private final boolean barrel;
        private final double[] vertices;
        private final double minX;
        private final double minY;
        private final double minZ;
        private final double maxX;
        private final double maxY;
        private final double maxZ;
        private final boolean convex;

        private Shape(EnumDriveablePart part, double[] vertices)
        {
            this.part = part == null ? EnumDriveablePart.CORE : part;
            this.turret = isTurretMountedPart(this.part);
            this.barrel = isBarrelPart(this.part);
            this.vertices = vertices.clone();

            double localMinX = Double.POSITIVE_INFINITY;
            double localMinY = Double.POSITIVE_INFINITY;
            double localMinZ = Double.POSITIVE_INFINITY;
            double localMaxX = Double.NEGATIVE_INFINITY;
            double localMaxY = Double.NEGATIVE_INFINITY;
            double localMaxZ = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < 8; i++)
            {
                localMinX = Math.min(localMinX, vertex(i, 0));
                localMinY = Math.min(localMinY, vertex(i, 1));
                localMinZ = Math.min(localMinZ, vertex(i, 2));
                localMaxX = Math.max(localMaxX, vertex(i, 0));
                localMaxY = Math.max(localMaxY, vertex(i, 1));
                localMaxZ = Math.max(localMaxZ, vertex(i, 2));
            }
            minX = localMinX;
            minY = localMinY;
            minZ = localMinZ;
            maxX = localMaxX;
            maxY = localMaxY;
            maxZ = localMaxZ;
            convex = calculateConvexity();
        }

        private boolean calculateConvexity()
        {
            double centreX = 0D;
            double centreY = 0D;
            double centreZ = 0D;
            for (int vertex = 0; vertex < 8; vertex++)
            {
                centreX += vertex(vertex, 0);
                centreY += vertex(vertex, 1);
                centreZ += vertex(vertex, 2);
            }
            centreX /= 8D;
            centreY /= 8D;
            centreZ /= 8D;
            double[] plane = new double[4];
            for (int[] face : FACE_QUADS)
            {
                if (!facePlane(vertices, face, centreX, centreY, centreZ, plane))
                    return false;
                for (int vertex = 0; vertex < 8; vertex++)
                {
                    double signed = plane[0] * vertex(vertex, 0) + plane[1] * vertex(vertex, 1)
                        + plane[2] * vertex(vertex, 2) + plane[3];
                    if (signed > 1.0E-4D)
                        return false;
                }
            }
            return true;
        }

        double[] coordinates()
        {
            return vertices;
        }

        double vertex(int index, int axis)
        {
            return vertices[index * 3 + axis];
        }

        public EnumDriveablePart getPart() { return part; }
        public boolean isTurret() { return turret; }
        public boolean isBarrel() { return barrel; }
        public boolean isConvex() { return convex; }
        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }
    }

    /** Shared turret classification for collision, projectile hits and armour precedence. */
    public static boolean isTurretMountedPart(EnumDriveablePart part)
    {
        return part == EnumDriveablePart.TURRET || part == EnumDriveablePart.BARREL
            || part != null && part.name().startsWith("TURRET_");
    }

    /** Damageable gun/barrel boxes follow both turret yaw and barrel pitch. */
    public static boolean isBarrelPart(EnumDriveablePart part)
    {
        return part == EnumDriveablePart.BARREL;
    }

    /**
     * Produces an outward unit plane for a quad. The output is n.xyz followed
     * by d for {@code dot(n, point) + d = 0}.
     */
    static boolean facePlane(double[] points, int[] face, double centreX, double centreY, double centreZ,
                             double[] output)
    {
        int a = face[0] * 3;
        int b = face[1] * 3;
        int c = face[2] * 3;
        int d = face[3] * 3;

        double abX = points[b] - points[a];
        double abY = points[b + 1] - points[a + 1];
        double abZ = points[b + 2] - points[a + 2];
        double acX = points[c] - points[a];
        double acY = points[c + 1] - points[a + 1];
        double acZ = points[c + 2] - points[a + 2];
        double adX = points[d] - points[a];
        double adY = points[d + 1] - points[a + 1];
        double adZ = points[d + 2] - points[a + 2];

        // Average both triangle normals to remain stable for slightly warped
        // legacy shape-box faces.
        double nX = abY * acZ - abZ * acY + acY * adZ - acZ * adY;
        double nY = abZ * acX - abX * acZ + acZ * adX - acX * adZ;
        double nZ = abX * acY - abY * acX + acX * adY - acY * adX;
        double length = Math.sqrt(nX * nX + nY * nY + nZ * nZ);
        if (!Double.isFinite(length) || length < 1.0E-8D)
            return false;
        nX /= length;
        nY /= length;
        nZ /= length;

        double faceX = (points[a] + points[b] + points[c] + points[d]) * 0.25D;
        double faceY = (points[a + 1] + points[b + 1] + points[c + 1] + points[d + 1]) * 0.25D;
        double faceZ = (points[a + 2] + points[b + 2] + points[c + 2] + points[d + 2]) * 0.25D;
        if (nX * (faceX - centreX) + nY * (faceY - centreY) + nZ * (faceZ - centreZ) < 0D)
        {
            nX = -nX;
            nY = -nY;
            nZ = -nZ;
        }
        output[0] = nX;
        output[1] = nY;
        output[2] = nZ;
        output[3] = -(nX * points[a] + nY * points[a + 1] + nZ * points[a + 2]);
        return true;
    }
}
