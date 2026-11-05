package com.flansmodultimate.common.types.keyframe;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A keyframe animation from a Bedrock-style {@code .animation.json} file, as used by the Tyrants and Plebeians
 * "GLTF" animation system. Bones are named after the gun parts they move.
 *
 * @param length animation length in seconds
 */
public record KeyframeAnimation(float length, Map<String, KeyframeBone> bones)
{
    public KeyframeAnimation
    {
        bones = Map.copyOf(bones);
    }

    @Nullable
    public KeyframeBone bone(EnumKeyframeBone part)
    {
        return bones.get(part.getBoneName());
    }

    /** Position and rotation keyframes of one bone, in model pixels and degrees. */
    public record KeyframeBone(KeyframeChannel position, KeyframeChannel rotation)
    {}
}
