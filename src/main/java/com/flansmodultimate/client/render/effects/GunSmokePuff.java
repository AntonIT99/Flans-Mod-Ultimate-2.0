package com.flansmodultimate.client.render.effects;

import com.flansmod.client.model.ModelMuzzleSmoke;
import com.flansmod.common.vector.Vector3f;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * One fading smoke puff of the Tyrants and Plebeians gun smoke: a muzzle puff that stays on the gun, or a chamber puff
 * that drifts away from where the gun fired it. Positions are in first-person render space.
 */
@Getter
public final class GunSmokePuff
{
    @Nullable
    private ModelMuzzleSmoke model;
    @Nullable
    private ResourceLocation texture;
    private float scale;
    private float scaleGrowth;
    private float vanishRate;
    private float rotationX;
    private final Vector3f position = new Vector3f();
    private final Vector3f lastPosition = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private boolean drifting;
    private float spawnYaw;
    private float spawnPitch;
    private float alpha = 1F;

    /** Starts a puff that stays where it spawned. */
    public void spawn(ModelMuzzleSmoke smokeModel, @Nullable ResourceLocation smokeTexture, float startScale, float growth, Vector3f at, float rotX, float vanish)
    {
        model = smokeModel;
        texture = smokeTexture;
        scale = startScale;
        scaleGrowth = growth;
        position.set(at.x, at.y, at.z);
        lastPosition.set(at.x, at.y, at.z);
        velocity.set(0F, 0F, 0F);
        drifting = false;
        rotationX = rotX;
        vanishRate = vanish;
        alpha = 1F;
    }

    /** Starts a puff that drifts with {@code startVelocity}, remembering the view it was fired from. */
    @SuppressWarnings("java:S107") // Mirrors the particle spawn state of the other effect helpers
    public void spawnDrifting(ModelMuzzleSmoke smokeModel, @Nullable ResourceLocation smokeTexture, float startScale, float growth, Vector3f at, Vector3f startVelocity, float rotX, float vanish,
        float yaw, float pitch)
    {
        spawn(smokeModel, smokeTexture, startScale, growth, at, rotX, vanish);
        velocity.set(startVelocity.x, startVelocity.y, startVelocity.z);
        drifting = true;
        spawnYaw = yaw;
        spawnPitch = pitch;
    }

    public boolean isAlive()
    {
        return model != null;
    }

    /** One client tick: fades, follows the player's movement by {@code (dx, dy, dz)} and drifts. */
    public void tick(float dx, float dy, float dz)
    {
        if (model == null)
            return;

        alpha *= vanishRate;
        lastPosition.set(position.x, position.y, position.z);
        position.translate(dx, dy, dz);
        if (drifting)
        {
            position.translate(velocity.x, velocity.y, velocity.z);
            velocity.y += 0.0002F;
            velocity.scale(0.96F);
        }
        if (alpha < 0.02F)
        {
            model = null;
            drifting = false;
        }
    }

    /** The puff grows as it fades. */
    public float currentScale()
    {
        return scale + scaleGrowth * (1F - alpha);
    }
}
