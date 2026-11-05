package com.flansmodultimate.client.render.effects;

import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A Tyrants and Plebeians "fancy" spent casing, or ejected en-bloc clip: a model thrown from the gun that falls, slows
 * and spins in first-person render space for its lifetime.
 */
@Getter
public final class GunCasingParticle
{
    @Nullable
    private ModelCasing casingModel;
    @Nullable
    private ModelRendererTurbo[] clipModel;
    @Nullable
    private ResourceLocation texture;
    private final Vector3f position = new Vector3f();
    private final Vector3f lastPosition = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private final Vector3f rotation = new Vector3f();
    private final Vector3f angularVelocity = new Vector3f();
    private float scale = 1F;
    private int age;
    private int lifetime;
    private float gravity;
    private float drag;
    private float spawnYaw;
    private float spawnPitch;
    private boolean alive;

    /** Throws a casing model. */
    @SuppressWarnings("java:S107") // Mirrors the Tyrants and Plebeians casing parameters one to one
    public void spawn(@Nullable ModelCasing model, @Nullable ModelRendererTurbo[] clip, @Nullable ResourceLocation modelTexture, Vector3f at, Vector3f startVelocity, Vector3f spin,
        float modelScale, int life, float yaw, float pitch, float fallSpeed, float slowdown)
    {
        alive = true;
        casingModel = model;
        clipModel = clip;
        texture = modelTexture;
        position.set(at.x, at.y, at.z);
        lastPosition.set(at.x, at.y, at.z);
        velocity.set(startVelocity.x, startVelocity.y, startVelocity.z);
        angularVelocity.set(spin.x, spin.y, spin.z);
        rotation.set(0F, 0F, 0F);
        scale = modelScale;
        lifetime = life;
        age = 0;
        gravity = fallSpeed;
        drag = slowdown;
        spawnYaw = yaw;
        spawnPitch = pitch;
    }

    /** One client tick: follows the player's movement by {@code (dx, dy, dz)}, falls along the view it was fired from, slows and spins. */
    public void tick(float dx, float dy, float dz)
    {
        if (!alive)
            return;

        lastPosition.set(position.x, position.y, position.z);
        position.translate(dx, dy, dz);
        position.translate(velocity.x, velocity.y, velocity.z);

        // World-down gravity expressed in the pitch of the view the casing was fired from; it has no sideways part
        float pitch = (float) Math.toRadians(spawnPitch);
        velocity.x += gravity * Mth.sin(pitch);
        velocity.y -= gravity * Mth.cos(pitch);
        velocity.scale(drag);

        rotation.translate(angularVelocity.x, angularVelocity.y, angularVelocity.z);
        if (++age > lifetime)
        {
            alive = false;
            casingModel = null;
            clipModel = null;
        }
    }
}
