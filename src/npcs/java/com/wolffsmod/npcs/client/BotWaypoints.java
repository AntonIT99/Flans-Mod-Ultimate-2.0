package com.wolffsmod.npcs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.npcs.network.BotWaypointsPacket;
import com.wolffsmod.npcs.platform.client.NpcClientPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Marks in the world where the player's orders send their squads, and the rally point of the squad they lead: a label
 * with the squad, what it does there and how far away it is, seen through walls and kept readable at any distance.
 * Client side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotWaypoints
{
    /** Blocks from the camera beyond which a label is drawn closer, in its direction, so it stays in view. */
    private static final double FARTHEST = 32D;
    /** Blocks above its place a label floats. */
    private static final double LIFT = 2D;
    private static final float SCALE = 0.025F;
    /** Blocks within which a label keeps its size; farther, it grows so it reads as large. */
    private static final double NEAR = 6D;
    private static final int COLOUR = 0xFFFFFF55;
    private static final int RALLY_COLOUR = 0xFF55FFFF;
    private static final int BACKGROUND = 0x40000000;

    private static List<BotWaypointsPacket.Waypoint> waypoints = List.of();

    public static void receive(BotWaypointsPacket packet)
    {
        waypoints = packet.waypoints();
    }

    public static void clear()
    {
        waypoints = List.of();
    }

    /** Draws the labels, after the world, in the camera-relative pose of the level renderer. */
    public static void render(PoseStack pose, Camera camera)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (waypoints.isEmpty() || minecraft.player == null)
            return;
        Font font = minecraft.font;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Vec3 eye = camera.getPosition();
        for (BotWaypointsPacket.Waypoint waypoint : waypoints)
        {
            Vec3 at = new Vec3(waypoint.x(), waypoint.y() + LIFT, waypoint.z());
            double distance = at.distanceTo(eye);
            Vec3 offset = at.subtract(eye);
            if (distance > FARTHEST)
                offset = offset.scale(FARTHEST / distance);
            double shown = Math.min(distance, FARTHEST);
            float scale = SCALE * (float) Math.max(1D, shown / NEAR);
            Component label = Component.translatable("wolffsmodnpcs.waypoint." + waypoint.kind(), "#" + waypoint.squad(), (int) Math.round(distance));
            pose.pushPose();
            pose.translate(offset.x, offset.y, offset.z);
            pose.mulPose(camera.rotation());
            NpcClientPlatform.scaleCameraFacingLabel(pose, scale);
            Matrix4f matrix = pose.last().pose();
            int colour = "rally".equals(waypoint.kind()) ? RALLY_COLOUR : COLOUR;
            font.drawInBatch(label, -font.width(label) / 2F, 0F, colour, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, BACKGROUND, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
