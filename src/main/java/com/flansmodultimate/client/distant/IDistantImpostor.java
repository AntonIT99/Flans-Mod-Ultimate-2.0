package com.flansmodultimate.client.distant;

import com.flansmodultimate.client.render.entity.DriveableImpostorCache;
import org.joml.Quaternionf;

import net.minecraft.world.phys.Vec3;

/** A distant driveable drawn as an impostor of its model rather than as boxes. Render thread. */
public interface IDistantImpostor
{
    DriveableImpostorCache.DistantAtlas atlas();

    /** World position of the model's origin at this point of the current tick. */
    Vec3 at(float partialTick);

    /** Writes the model's rotation about its origin at this point of the current tick into {@code out}, and returns it. */
    Quaternionf rotation(float partialTick, Quaternionf out);

    /** Blocks by which the billboard is pulled towards the camera, so that the ground it stands on does not cut into it. */
    float pull();
}
