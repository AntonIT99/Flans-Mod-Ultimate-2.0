package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.phys.Vec3;

/**
 * Converts coordinates authored for the 1.7.10 model basis into the modern
 * driveable basis. Legacy type files use model X as forward and model Z as
 * lateral; modern entity physics uses local X as forward and local Z as right.
 */
public final class LegacyDriveableCoordinates
{
    private LegacyDriveableCoordinates() {}

    public static Vec3 toLocal(@NotNull Vec3 legacy)
    {
        return new Vec3(legacy.z, legacy.y, -legacy.x);
    }

    public static Vec3 toLocal(@NotNull Vector3f legacy)
    {
        return new Vec3(legacy.z, legacy.y, -legacy.x);
    }

    /** Applies the basis conversion to packed xyz vertices in place. */
    public static void toLocalVertices(double @NotNull [] vertices)
    {
        for (int index = 0; index + 2 < vertices.length; index += 3)
        {
            double legacyX = vertices[index];
            vertices[index] = vertices[index + 2];
            vertices[index + 2] = -legacyX;
        }
    }
}
