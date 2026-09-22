package com.flansmodultimate.common.explosions;

/**
 * How far an explosion reaches in every direction, sampled on a cube map and read back with
 * bilinear interpolation.
 * <p>
 * Rays alone cannot carve a big crater: however many are cast, they spread apart with distance
 * and leave pillars between them, and wherever the rays are densest the crater looks most
 * complete, which on a cube-shell ray pattern means a square. Instead each ray only measures its
 * reach, and every block inside the crater radius is tested against the reach interpolated for
 * its own direction. The crater is then solid out to a smooth boundary whatever the ray spacing,
 * and the ray pattern no longer shows through.
 * <p>
 * Face {@code f} is axis {@code f / 2} (x, y, z) on the positive side when {@code f} is even. The
 * two remaining axes, in x-y-z order, are the face's u and v coordinates.
 */
public final class CraterReachMap
{
    private final int resolution;
    private final float[] reach;

    public CraterReachMap(int resolution)
    {
        if (resolution < 1)
            throw new IllegalArgumentException("resolution must be positive: " + resolution);
        this.resolution = resolution;
        reach = new float[6 * resolution * resolution];
    }

    public int resolution()
    {
        return resolution;
    }

    public int cellCount()
    {
        return reach.length;
    }

    /** Unit direction through the centre of a cell, written into {@code out} as x, y, z. */
    public void cellDirection(int cell, double[] out)
    {
        int face = cell / (resolution * resolution);
        int local = cell % (resolution * resolution);
        double u = ((double) local / resolution + 0.5D) / resolution * 2D - 1D;
        double v = (local % resolution + 0.5D) / resolution * 2D - 1D;
        double major = (face & 1) == 0 ? 1D : -1D;

        double x;
        double y;
        double z;
        switch (face / 2)
        {
            case 0 -> { x = major; y = u; z = v; }
            case 1 -> { x = u; y = major; z = v; }
            default -> { x = u; y = v; z = major; }
        }
        double invLen = 1D / Math.sqrt(x * x + y * y + z * z);
        out[0] = x * invLen;
        out[1] = y * invLen;
        out[2] = z * invLen;
    }

    public void setReach(int cell, float distance)
    {
        reach[cell] = distance;
    }

    /** Interpolated reach towards the direction {@code (dx, dy, dz)}, which need not be normalised. */
    public float reachTowards(double dx, double dy, double dz)
    {
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);

        int face;
        double major;
        double a;
        double b;
        if (ax >= ay && ax >= az)
        {
            face = dx >= 0D ? 0 : 1;
            major = ax;
            a = dy;
            b = dz;
        }
        else if (ay >= az)
        {
            face = dy >= 0D ? 2 : 3;
            major = ay;
            a = dx;
            b = dz;
        }
        else
        {
            face = dz >= 0D ? 4 : 5;
            major = az;
            a = dx;
            b = dy;
        }
        if (major <= 0D)
            return reach[0];

        // Continuous cell coordinates, where cell i's centre sits at exactly i. Clamping at the
        // face border holds the outermost row flat for the last half cell rather than blending
        // across faces; the neighbouring face's own outermost row sits right next to it, so the
        // seam this leaves is a fraction of one cell.
        double fu = clamp(((a / major) + 1D) * 0.5D * resolution - 0.5D);
        double fv = clamp(((b / major) + 1D) * 0.5D * resolution - 0.5D);
        int u0 = (int) fu;
        int v0 = (int) fv;
        int u1 = Math.min(u0 + 1, resolution - 1);
        int v1 = Math.min(v0 + 1, resolution - 1);
        double tu = fu - u0;
        double tv = fv - v0;

        int base = face * resolution * resolution;
        double r00 = reach[base + u0 * resolution + v0];
        double r01 = reach[base + u0 * resolution + v1];
        double r10 = reach[base + u1 * resolution + v0];
        double r11 = reach[base + u1 * resolution + v1];
        double lowU = r00 + (r01 - r00) * tv;
        double highU = r10 + (r11 - r10) * tv;
        return (float) (lowU + (highU - lowU) * tu);
    }

    private double clamp(double coordinate)
    {
        return Math.max(0D, Math.min(resolution - 1D, coordinate));
    }
}
