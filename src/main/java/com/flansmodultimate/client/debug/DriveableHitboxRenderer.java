package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.DriveableProjectileCollision;
import com.flansmodultimate.common.entity.Driveable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Outlines the per-part collision boxes of driveables in debug mode.
 * Boxes go through the same hull and turret transforms as projectile tracing,
 * so what is drawn is what bullets actually hit.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DriveableHitboxRenderer
{
    /** Corner pairs of a box's 12 edges; corner bit 1 selects max X, bit 2 max Y and bit 4 max Z. */
    private static final int[][] EDGES = {
        {0, 1}, {2, 3}, {4, 5}, {6, 7},
        {0, 2}, {1, 3}, {4, 6}, {5, 7},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    /**
     * Draws the hitboxes of every visible driveable and flushes them immediately.
     * Must run once entities have been drawn, otherwise the models are drawn over the outlines.
     */
    public static void renderAll(@NotNull PoseStack poseStack, @NotNull MultiBufferSource.BufferSource buffer, @NotNull Camera camera, @NotNull Frustum frustum, float partialTick)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (!ModClient.isDebug() || level == null)
            return;

        Vec3 cameraPosition = camera.getPosition();
        VertexConsumer lines = buffer.getBuffer(CustomRenderType.debugLinesSeeThrough());
        for (Entity entity : level.entitiesForRendering())
        {
            if (!(entity instanceof Driveable driveable) || !frustum.isVisible(driveable.getBoundingBoxForCulling()))
                continue;

            Vec3 origin = driveable.getPosition(partialTick).subtract(cameraPosition);
            poseStack.pushPose();
            poseStack.translate(origin.x, origin.y, origin.z);
            render(driveable, poseStack.last(), lines);
            poseStack.popPose();
        }
        buffer.endBatch(CustomRenderType.debugLinesSeeThrough());
    }

    /** Expects {@code pose} to be translated to the driveable's origin, with no rotation applied. */
    private static void render(Driveable driveable, PoseStack.Pose pose, VertexConsumer lines)
    {
        DriveableData data = driveable.getDriveableData();
        if (data == null)
            return;

        Vec3 turretPivot = driveable.getCollisionTurretPivot();
        Vec3 turretOffset = driveable.getCollisionTurretOffset();
        Vec3[] corners = new Vec3[8];
        for (DriveablePart part : data.getParts().values())
        {
            CollisionBox box = part.getBox();
            if (box == null)
                continue;

            AABB bounds = box.asAabb();
            for (int corner = 0; corner < corners.length; corner++)
            {
                Vec3 partLocal = new Vec3((corner & 1) == 0 ? bounds.minX : bounds.maxX,
                    (corner & 2) == 0 ? bounds.minY : bounds.maxY, (corner & 4) == 0 ? bounds.minZ : bounds.maxZ);
                Vec3 hullLocal = DriveableProjectileCollision.partPointToHullLocal(partLocal, part.getType(),
                    driveable.getTurretYaw(), driveable.getTurretPitch(), turretPivot, turretOffset);
                corners[corner] = driveable.localDirectionToWorld(hullLocal);
            }

            // Legacy colours: yellow while projectiles can hit the part, red once they pass through it
            float green = driveable.canHitPart(part.getType()) && driveable.isPartHitboxActive(part) ? 1F : 0F;
            for (int[] edge : EDGES)
                addLine(pose, lines, corners[edge[0]], corners[edge[1]], 1F, green, 0F);
        }
    }

    private static void addLine(PoseStack.Pose pose, VertexConsumer consumer, Vec3 from, Vec3 to, float red, float green, float blue)
    {
        Vec3 normal = to.subtract(from).normalize();
        consumer.addVertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z).setColor(red, green, blue, 1F)
            .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z).setColor(red, green, blue, 1F)
            .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
