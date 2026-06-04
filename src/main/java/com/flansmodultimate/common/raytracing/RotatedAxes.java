package com.flansmodultimate.common.raytracing;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.util.Mth;

public class RotatedAxes
{
    private float rotationYaw;
    private float rotationPitch;
    private float rotationRoll;
    private Matrix4f rotationMatrix;

    public RotatedAxes()
    {
        rotationMatrix = new Matrix4f();
    }

    public RotatedAxes(Matrix4f mat)
    {
        rotationMatrix = new Matrix4f(mat);
        convertMatrixToAngles();
    }

    public RotatedAxes(float yaw, float pitch, float roll)
    {
        setAngles(yaw, pitch, roll);
    }

    @Override
    public RotatedAxes clone()
    {
        return new RotatedAxes(rotationMatrix);
    }

    public boolean isValid()
    {
        float determinant = rotationMatrix.determinant();
        return determinant != 0F && !Float.isNaN(determinant);
    }

    public void setAngles(float yaw, float pitch, float roll)
    {
        rotationYaw = yaw;
        rotationPitch = pitch;
        rotationRoll = roll;
        convertAnglesToMatrix();
    }

    public float getYaw()
    {
        return rotationYaw;
    }

    public float getPitch()
    {
        return rotationPitch;
    }

    public float getRoll()
    {
        return rotationRoll;
    }

    public Vector3f getXAxis()
    {
        return new Vector3f(rotationMatrix.m00(), rotationMatrix.m10(), rotationMatrix.m20());
    }

    public Vector3f getYAxis()
    {
        return new Vector3f(rotationMatrix.m01(), rotationMatrix.m11(), rotationMatrix.m21());
    }

    public Vector3f getZAxis()
    {
        return new Vector3f(-rotationMatrix.m02(), -rotationMatrix.m12(), -rotationMatrix.m22());
    }

    public Matrix4f getMatrix()
    {
        return rotationMatrix;
    }

    public void rotateLocalYaw(float rotateBy)
    {
        rotationMatrix.rotate(rotateBy * Mth.PI / 180F, getYAxis().normalize());
        convertMatrixToAngles();
    }

    public void rotateLocalPitch(float rotateBy)
    {
        rotationMatrix.rotate(rotateBy * Mth.PI / 180F, getZAxis().normalize());
        convertMatrixToAngles();
    }

    public void rotateLocalRoll(float rotateBy)
    {
        rotationMatrix.rotate(rotateBy * Mth.PI / 180F, getXAxis().normalize());
        convertMatrixToAngles();
    }

    public RotatedAxes rotateGlobalYaw(float rotateBy)
    {
        rotationMatrix.rotate(rotateBy * Mth.PI / 180F, 0F, 1F, 0F);
        convertMatrixToAngles();
        return this;
    }

    public RotatedAxes rotateGlobalPitch(float rotateBy)
    {
        rotationMatrix.rotate(rotateBy * Mth.PI / 180F, 0F, 0F, 1F);
        convertMatrixToAngles();
        return this;
    }

    public RotatedAxes rotateGlobalRoll(float rotateBy)
    {
        rotationMatrix.rotate(rotateBy * Mth.PI / 180F, 1F, 0F, 0F);
        convertMatrixToAngles();
        return this;
    }

    public RotatedAxes rotateGlobalYawInRads(float rotateBy)
    {
        rotationMatrix.rotate(rotateBy, 0F, 1F, 0F);
        convertMatrixToAngles();
        return this;
    }

    public RotatedAxes rotateGlobalPitchInRads(float rotateBy)
    {
        rotationMatrix.rotate(rotateBy, 0F, 0F, 1F);
        convertMatrixToAngles();
        return this;
    }

    public RotatedAxes rotateGlobalRollInRads(float rotateBy)
    {
        rotationMatrix.rotate(rotateBy, 1F, 0F, 0F);
        convertMatrixToAngles();
        return this;
    }

    public void rotateLocal(float rotateBy, Vector3f rotateAround)
    {
        rotationMatrix.rotate(rotateBy * Mth.PI / 180F, findLocalVectorGlobally(rotateAround));
        convertMatrixToAngles();
    }

    public void rotateGlobal(float rotateBy, Vector3f rotateAround)
    {
        rotationMatrix.rotate(rotateBy * Mth.PI / 180F, rotateAround);
        convertMatrixToAngles();
    }

    public Vector3f findGlobalVectorLocally(Vector3f in)
    {
        Matrix4f mat = new Matrix4f().m00(in.x).m10(in.y).m20(in.z);
        mat.rotate(-rotationYaw * Mth.PI / 180F, 0F, 1F, 0F);
        mat.rotate(-rotationPitch * Mth.PI / 180F, 0F, 0F, 1F);
        mat.rotate(-rotationRoll * Mth.PI / 180F, 1F, 0F, 0F);
        return new Vector3f(mat.m00(), mat.m10(), mat.m20());
    }

    public Vector3f findLocalVectorGlobally(Vector3f in)
    {
        Matrix4f mat = new Matrix4f().m00(in.x).m10(in.y).m20(in.z);
        mat.rotate(rotationRoll * Mth.PI / 180F, 1F, 0F, 0F);
        mat.rotate(rotationPitch * Mth.PI / 180F, 0F, 0F, 1F);
        mat.rotate(rotationYaw * Mth.PI / 180F, 0F, 1F, 0F);
        return new Vector3f(mat.m00(), mat.m10(), mat.m20());
    }

    private void convertAnglesToMatrix()
    {
        rotationMatrix = new Matrix4f();
        rotationMatrix.rotate(rotationRoll * Mth.PI / 180F, 1F, 0F, 0F);
        rotationMatrix.rotate(rotationPitch * Mth.PI / 180F, 0F, 0F, 1F);
        rotationMatrix.rotate(rotationYaw * Mth.PI / 180F, 0F, 1F, 0F);
        convertMatrixToAngles();
    }

    private void convertMatrixToAngles()
    {
        rotationYaw = (float)Math.atan2(rotationMatrix.m20(), rotationMatrix.m00()) * 180F / Mth.PI;
        rotationPitch = (float)Math.atan2(-rotationMatrix.m10(), Math.sqrt(rotationMatrix.m12() * rotationMatrix.m12() + rotationMatrix.m11() * rotationMatrix.m11())) * 180F / Mth.PI;
        rotationRoll = (float)Math.atan2(rotationMatrix.m12(), rotationMatrix.m11()) * 180F / Mth.PI;
    }

    public RotatedAxes findLocalAxesGlobally(RotatedAxes in)
    {
        Matrix4f mat = new Matrix4f(in.getMatrix());
        mat.rotate(rotationRoll * Mth.PI / 180F, 1F, 0F, 0F);
        mat.rotate(rotationPitch * Mth.PI / 180F, 0F, 0F, 1F);
        mat.rotate(rotationYaw * Mth.PI / 180F, 0F, 1F, 0F);
        return new RotatedAxes(mat);
    }

    @Override
    public String toString()
    {
        return "RotatedAxes[Yaw = " + getYaw() + ", Pitch = " + getPitch() + ", Roll = " + getRoll() + "]";
    }
}
