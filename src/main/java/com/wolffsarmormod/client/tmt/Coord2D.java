package com.wolffsarmormod.client.tmt;

import net.minecraft.util.Mth;

/**
 * This class represents a coordinate space and its UV coordinates. This allows for
 * easier flat shape planning.
 *
 * @author GaryCXJk
 */
public class Coord2D
{
    public double xCoord;
    public double yCoord;
    public int uCoord;
    public int vCoord;

    public Coord2D(double x, double y)
    {
        xCoord = x;
        yCoord = y;
        uCoord = Mth.floor(x);
        vCoord = Mth.floor(y);
    }

    public Coord2D(double x, double y, int u, int v)
    {
        this(x, y);
        uCoord = u;
        vCoord = v;
    }
}
