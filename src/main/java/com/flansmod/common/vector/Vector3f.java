package com.flansmod.common.vector;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import net.minecraft.world.phys.Vec3;

import java.io.Serializable;
import java.nio.FloatBuffer;

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings({"unused", "java:S1104"})
public class Vector3f implements Serializable
{
    public static final Vector3f Zero = new Vector3f(0F, 0F, 0F);

    public float x;
    public float y;
    public float z;

    public Vector3f(String input)
    {
        //Input should be of the form [float,float,float]
        String noBrackets = input.substring(1, input.length() - 1);
        int firstComma = noBrackets.indexOf(',');
        int secondComma = noBrackets.indexOf(',', firstComma + 1);
        if (firstComma >= 0 && secondComma > firstComma && secondComma < noBrackets.length() - 1 && noBrackets.indexOf(',', secondComma + 1) < 0)
        {
            x = Float.parseFloat(noBrackets.substring(0, firstComma));
            y = Float.parseFloat(noBrackets.substring(firstComma + 1, secondComma));
            z = Float.parseFloat(noBrackets.substring(secondComma + 1));
        }
    }

    public Vector3f(float x, float y, float z)
    {
        set(x, y, z);
    }

    public Vector3f(Vec3 vec)
    {
        this((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public Vector3f(double x, double y, double z)
    {
        this((float) x, (float) y, (float) z);
    }

    public Vec3 toVec3()
    {
        return new Vec3(x, y, z);
    }

    public void set(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @return the length of the vector
     */
    public final float length()
    {
        return (float) Math.sqrt(lengthSquared());
    }

    /**
     * @return the length squared of the vector
     */
    public float lengthSquared()
    {
        return x * x + y * y + z * z;
    }

    /**
     * Translate a vector
     *
     * @param x The translation in x
     * @param y the translation in y
     * @return this
     */
    public Vector3f translate(float x, float y, float z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    /**
     * Add a vector to another vector and place the result in a destination
     * vector.
     *
     * @param left  The LHS vector
     * @param right The RHS vector
     * @param dest  The destination vector, or null if a new vector is to be created
     * @return the sum of left and right in dest
     */
    public static Vector3f add(Vector3f left, Vector3f right, Vector3f dest)
    {
        if (dest == null)
            return new Vector3f(left.x + right.x, left.y + right.y, left.z + right.z);
        else
        {
            dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
            return dest;
        }
    }

    /**
     * Subtract a vector from another vector and place the result in a destination
     * vector.
     *
     * @param left  The LHS vector
     * @param right The RHS vector
     * @param dest  The destination vector, or null if a new vector is to be created
     * @return left minus right in dest
     */
    public static Vector3f sub(Vector3f left, Vector3f right, Vector3f dest)
    {
        if (dest == null)
            return new Vector3f(left.x - right.x, left.y - right.y, left.z - right.z);
        else
        {
            dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
            return dest;
        }
    }

    /**
     * The cross product of two vectors.
     *
     * @param left  The LHS vector
     * @param right The RHS vector
     * @param dest  The destination result, or null if a new vector is to be created
     * @return left cross right
     */
    public static Vector3f cross(Vector3f left, Vector3f right, Vector3f dest)
    {

        if (dest == null)
            dest = new Vector3f();

        dest.set(
            left.y * right.z - left.z * right.y,
            right.x * left.z - right.z * left.x,
            left.x * right.y - left.y * right.x
        );

        return dest;
    }


    /**
     * Negate a vector
     *
     * @return this
     */
    public Vector3f negate()
    {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    /**
     * Negate a vector and place the result in a destination vector.
     *
     * @param dest The destination vector or null if a new vector is to be created
     * @return the negated vector
     */
    public Vector3f negate(Vector3f dest)
    {
        if (dest == null)
            dest = new Vector3f();
        dest.x = -x;
        dest.y = -y;
        dest.z = -z;
        return dest;
    }

    /**
     * Normalise this vector and place the result in another vector.
     *
     * @param dest The destination vector, or null if a new vector is to be created
     * @return the normalised vector
     */
    public Vector3f normalise(Vector3f dest)
    {
        float l = length();
        float inverseLength = 1.0F / l;

        if (dest == null)
            dest = new Vector3f(x * inverseLength, y * inverseLength, z * inverseLength);
        else
            dest.set(x * inverseLength, y * inverseLength, z * inverseLength);

        return dest;
    }

    /**
     * The dot product of two vectors is calculated as
     * v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
     *
     * @param left  The LHS vector
     * @param right The RHS vector
     * @return left dot right
     */
    public static float dot(Vector3f left, Vector3f right)
    {
        return left.x * right.x + left.y * right.y + left.z * right.z;
    }

    /**
     * Calculate the angle between two vectors, in radians
     *
     * @param a A vector
     * @param b The other vector
     * @return the angle between the two vectors, in radians
     */
    public static float angle(Vector3f a, Vector3f b)
    {
        float dls = (float) (dot(a, b) / Math.sqrt(a.lengthSquared() * b.lengthSquared()));
        if (dls < -1f)
            dls = -1f;
        else if (dls > 1.0f)
            dls = 1.0f;
        return (float) Math.acos(dls);
    }

    public Vector3f load(FloatBuffer buf)
    {
        x = buf.get();
        y = buf.get();
        z = buf.get();
        return this;
    }

    public Vector3f scale(float scale)
    {
        x *= scale;
        y *= scale;
        z *= scale;
        return this;
    }

    public Vector3f store(FloatBuffer buf)
    {
        buf.put(x);
        buf.put(y);
        buf.put(z);
        return this;
    }

    @Override
    public String toString()
    {
        return "Vector3f[" + x + ", " + y + ", " + z + ']';
    }

    @Override
    public boolean equals(Object obj)
    {
        return (obj instanceof Vector3f vector3f) && vector3f.x == this.x && vector3f.y == this.y && vector3f.z == this.z;
    }

    @Override
    public int hashCode()
    {
        int result = Float.hashCode(x);
        result = 31 * result + Float.hashCode(y);
        result = 31 * result + Float.hashCode(z);
        return result;
    }
}
