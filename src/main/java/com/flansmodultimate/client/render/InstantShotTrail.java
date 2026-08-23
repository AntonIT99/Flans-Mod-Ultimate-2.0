package com.flansmodultimate.client.render;

import com.flansmodultimate.util.JomlUtils;
import org.joml.Vector3f;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class InstantShotTrail
{
    private final Vec3 origin;
    private final Vec3 hitPos;
    private final float width;
    private final float length;
    private final float bulletSpeed; // blocks per tick
    private final double distanceToTarget;
    private int ticksExisted;
    private final Identifier texture;

    /**
     * @param origin       world-space start
     * @param hitPos       world-space end
     * @param width        trail width (blocks)
     * @param length       visible length (blocks)
     * @param bulletSpeed  blocks per tick (client-simulated travel)
     * @param trailTexture texture RL (e.g., "modid:textures/misc/trail.png")
     */
    public InstantShotTrail(Vec3 origin, Vec3 hitPos, float width, float length, float bulletSpeed, Identifier trailTexture)
    {
        this.origin = origin;
        this.hitPos = hitPos;
        this.width = width;
        this.length = length;
        this.bulletSpeed = bulletSpeed;
        this.ticksExisted = 0;
        this.texture = trailTexture;

        Vec3 dPos = hitPos.subtract(origin);
        double dist = dPos.length();
        if (Math.abs(dist) > 300.0f)
            dist = 300.0f;
        this.distanceToTarget = dist;
    }

    /** Return true if this needs deleting */
    public boolean update()
    {
        ticksExisted++;
        return ticksExisted * bulletSpeed >= distanceToTarget - length;
    }

    public Snapshot extract(float partialTicks, Vec3 cameraPosition)
    {
        float parametric = (ticksExisted + partialTicks) * bulletSpeed;

        // Direction from origin to hit
        Vector3f dir = JomlUtils.fromVec3(hitPos.subtract(origin));
        if (dir.lengthSquared() == 0)
            return null;
        dir.normalize();

        float startT = parametric - length * 0.5f;
        float endT = parametric + length * 0.5f;

        float startX = (float) origin.x + dir.x * startT;
        float startY = (float) origin.y + dir.y * startT;
        float startZ = (float) origin.z + dir.z * startT;
        float endX = (float) origin.x + dir.x * endT;
        float endY = (float) origin.y + dir.y * endT;
        float endZ = (float) origin.z + dir.z * endT;

        // Build trail frame:
        // tangent is perpendicular to both (dir) and (toCamera)
        Vector3f toCam = new Vector3f((float) (cameraPosition.x - hitPos.x), (float) (cameraPosition.y - hitPos.y), (float) (cameraPosition.z - hitPos.z));
        Vector3f tangent = dir.cross(toCam, new Vector3f());
        if (tangent.lengthSquared() == 0)
            return null;
        tangent.normalize().mul(-width * 0.5f);

        Vector3f camera = JomlUtils.fromVec3(cameraPosition);
        return new Snapshot(texture,
            new Vector3f(startX + tangent.x, startY + tangent.y, startZ + tangent.z).sub(camera),
            new Vector3f(startX - tangent.x, startY - tangent.y, startZ - tangent.z).sub(camera),
            new Vector3f(endX - tangent.x, endY - tangent.y, endZ - tangent.z).sub(camera),
            new Vector3f(endX + tangent.x, endY + tangent.y, endZ + tangent.z).sub(camera));
    }

    public record Snapshot(Identifier texture, Vector3f startTop, Vector3f startBottom,
                           Vector3f endBottom, Vector3f endTop) {}
}
