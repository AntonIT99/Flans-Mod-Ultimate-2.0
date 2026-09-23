package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.common.raytracing.EnumHitboxType;
import com.flansmodultimate.common.raytracing.PlayerHitbox;
import com.flansmodultimate.common.raytracing.PlayerSnapshot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Outlines player hitboxes in debug mode.
 * A snapshot is rebuilt every frame at the render partial tick, so the outlines follow the rendered model
 * exactly while using the same hitbox geometry as bullet tracing.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerHitboxRenderer
{
    private static final float EDGE_THICKNESS = 0.02F;

    /** Corner pairs of a box's 12 edges; corner bit 1 selects max X, bit 2 max Y and bit 4 max Z. */
    private static final int[][] EDGES = {
        {0, 1}, {2, 3}, {4, 5}, {6, 7},
        {0, 2}, {1, 3}, {4, 6}, {5, 7},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    /** Draws the hitboxes of every visible player, except the camera player in first person, and flushes them. */
    public static void renderAll(@NotNull PoseStack poseStack, @NotNull MultiBufferSource.BufferSource buffer, @NotNull Camera camera, @NotNull Frustum frustum, float partialTick)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (!ModClient.isDebug() || level == null)
            return;

        Vec3 cameraPosition = camera.getPosition();
        VertexConsumer consumer = buffer.getBuffer(CustomRenderType.debugFilledBoxSeeThrough());
        for (Player player : level.players())
        {
            if ((player == camera.getEntity() && !camera.isDetached()) || !frustum.isVisible(player.getBoundingBoxForCulling().inflate(1D)))
                continue;

            // Offset from the double-precision position rather than the snapshot's float one to avoid jitter far from the origin
            Vec3 origin = new Vec3(Mth.lerp(partialTick, player.xOld, player.getX()), Mth.lerp(partialTick, player.yOld, player.getY()),
                Mth.lerp(partialTick, player.zOld, player.getZ())).subtract(cameraPosition);
            poseStack.pushPose();
            poseStack.translate(origin.x, origin.y, origin.z);
            for (PlayerHitbox hitbox : new PlayerSnapshot(player, partialTick).hitboxes)
                render(hitbox, poseStack, consumer);
            poseStack.popPose();
        }
        buffer.endBatch(CustomRenderType.debugFilledBoxSeeThrough());
    }

    /** Expects {@code poseStack} to be translated to the player's position, with no rotation applied. */
    private static void render(PlayerHitbox hitbox, PoseStack poseStack, VertexConsumer consumer)
    {
        Vector3f[] corners = new Vector3f[8];
        for (int corner = 0; corner < corners.length; corner++)
        {
            Vector3f local = new Vector3f(
                (corner & 1) == 0 ? hitbox.o.x : hitbox.o.x + hitbox.d.x,
                (corner & 2) == 0 ? hitbox.o.y : hitbox.o.y + hitbox.d.y,
                (corner & 4) == 0 ? hitbox.o.z : hitbox.o.z + hitbox.d.z);
            corners[corner] = hitbox.transform.transformPosition(local);
        }

        float[] color = color(hitbox.type);
        float halfThickness = EDGE_THICKNESS * 0.5F;
        for (int[] edge : EDGES)
        {
            Vector3f from = corners[edge[0]];
            Vector3f direction = new Vector3f(corners[edge[1]]).sub(from);
            float length = direction.length();
            if (length < 1.0E-6F)
                continue;

            direction.div(length);
            poseStack.pushPose();
            poseStack.translate(from.x, from.y, from.z);
            poseStack.mulPose(new Quaternionf().rotationTo(1F, 0F, 0F, direction.x, direction.y, direction.z));
            // Extend past both corners by half the thickness so adjoining edges close the corner
            LevelRenderer.addChainedFilledBoxVertices(poseStack, consumer, -halfThickness, -halfThickness, -halfThickness,
                length + halfThickness, halfThickness, halfThickness, color[0], color[1], color[2], 1F);
            poseStack.popPose();
        }
    }

    /** RGB per hitbox type, roughly following damage severity: red head, orange body, yellow arms, green legs, blue items. */
    private static float[] color(EnumHitboxType type)
    {
        return switch (type)
        {
            case HEAD -> new float[] {1F, 0.15F, 0.15F};
            case BODY -> new float[] {1F, 0.55F, 0F};
            case LEFTARM, RIGHTARM -> new float[] {1F, 0.9F, 0.1F};
            case LEGS -> new float[] {0.2F, 0.9F, 0.2F};
            case LEFTITEM, RIGHTITEM -> new float[] {0.2F, 0.6F, 1F};
        };
    }
}
