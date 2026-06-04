package com.flansmodultimate.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.world.phys.Vec3;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JomlUtils
{
    public static Vector3f fromVec3(Vec3 vec)
    {
        return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public static Vector3f fromFlansVector(com.flansmod.common.vector.Vector3f vec)
    {
        return new Vector3f(vec.x, vec.y, vec.z);
    }

    public static Vec3 toVec3(Vector3fc vec)
    {
        return new Vec3(vec.x(), vec.y(), vec.z());
    }
}
