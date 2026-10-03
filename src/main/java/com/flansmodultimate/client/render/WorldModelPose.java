package com.flansmodultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Measurements shared by live driveables and externally transformed static world models. */
public final class WorldModelPose
{
    private WorldModelPose() {}

    public static double originDistance(PoseStack pose)
    {
        var m = pose.last().pose();
        return Math.sqrt(m.m30()*m.m30() + m.m31()*m.m31() + m.m32()*m.m32());
    }

    /** Gershgorin bound on the largest singular value, including shear. */
    public static float scaleBound(PoseStack pose)
    {
        var m = pose.last().pose();
        float xx = m.m00()*m.m00() + m.m01()*m.m01() + m.m02()*m.m02();
        float yy = m.m10()*m.m10() + m.m11()*m.m11() + m.m12()*m.m12();
        float zz = m.m20()*m.m20() + m.m21()*m.m21() + m.m22()*m.m22();
        float xy = Math.abs(m.m00()*m.m10() + m.m01()*m.m11() + m.m02()*m.m12());
        float xz = Math.abs(m.m00()*m.m20() + m.m01()*m.m21() + m.m02()*m.m22());
        float yz = Math.abs(m.m10()*m.m20() + m.m11()*m.m21() + m.m12()*m.m22());
        return (float)Math.sqrt(Math.max(xx + xy + xz, Math.max(yy + xy + yz, zz + xz + yz)));
    }

    /** Positive uniform scale only; mirrors, shear and nonuniform scale require exact geometry. */
    public static float uniformScale(Matrix4f m)
    {
        if (!m.isFinite() || m.determinant3x3() <= 0F)
            return Float.NaN;
        float xx = m.m00()*m.m00() + m.m01()*m.m01() + m.m02()*m.m02();
        float yy = m.m10()*m.m10() + m.m11()*m.m11() + m.m12()*m.m12();
        float zz = m.m20()*m.m20() + m.m21()*m.m21() + m.m22()*m.m22();
        float tolerance = xx * 1E-4F;
        if (xx <= 1E-12F || Math.abs(xx - yy) > tolerance || Math.abs(xx - zz) > tolerance
            || Math.abs(m.m00()*m.m10() + m.m01()*m.m11() + m.m02()*m.m12()) > tolerance
            || Math.abs(m.m00()*m.m20() + m.m01()*m.m21() + m.m02()*m.m22()) > tolerance
            || Math.abs(m.m10()*m.m20() + m.m11()*m.m21() + m.m12()*m.m22()) > tolerance)
            return Float.NaN;
        return (float)Math.sqrt(xx);
    }

    /** Remove the accepted scale before extracting the parent rotation. */
    public static Quaternionf rotation(Matrix4f relative, float scale, Matrix4f scratch, Quaternionf destination)
    {
        return scratch.set(relative).scale(1F / scale).getNormalizedRotation(destination).normalize();
    }
}
