package com.flansmodultimate.client.render;

import com.flansmodultimate.common.item.ItemOpStick;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Draws the link an operator stick is holding open.
 *
 * <p>Between choosing the two ends of a connection there is nothing on screen to say which
 * base or object is already selected. 1.7.10 anchored a fishing line to it; this draws the
 * same line, from the holder to the waiting endpoint, straight from the stick's own data.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpStickConnectionRenderer
{
    private static final float RED = 0.2F;
    private static final float GREEN = 1.0F;
    private static final float BLUE = 0.4F;
    /** Where the line leaves the player: a little below eye level, as a held line would. */
    private static final double HAND_DROP = 0.4D;

    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource buffer, Camera camera, float partialTick)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        Optional<Vec3> endpoint = pendingEndpoint(player);
        if (endpoint.isEmpty())
            return;

        Vec3 cameraPosition = camera.getPosition();
        Vec3 from = player.getEyePosition(partialTick).subtract(0D, HAND_DROP, 0D).subtract(cameraPosition);
        Vec3 to = endpoint.get().subtract(cameraPosition);

        VertexConsumer lines = buffer.getBuffer(CustomRenderType.debugLinesSeeThrough());
        addLine(poseStack.last(), lines, from, to);
        buffer.endBatch(CustomRenderType.debugLinesSeeThrough());
    }

    /** The endpoint held by whichever hand carries an operator stick, if either does. */
    private static Optional<Vec3> pendingEndpoint(LocalPlayer player)
    {
        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof ItemOpStick))
                continue;
            Optional<Vec3> endpoint = ItemOpStick.getPendingConnection(stack);
            if (endpoint.isPresent())
                return endpoint;
        }
        return Optional.empty();
    }

    private static void addLine(PoseStack.Pose pose, VertexConsumer consumer, Vec3 from, Vec3 to)
    {
        Vec3 delta = to.subtract(from);
        if (delta.lengthSqr() < 1.0E-6D)
            return;
        Vec3 normal = delta.normalize();
        consumer.addVertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z).setColor(RED, GREEN, BLUE, 1F)
            .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z).setColor(RED, GREEN, BLUE, 1F)
            .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
