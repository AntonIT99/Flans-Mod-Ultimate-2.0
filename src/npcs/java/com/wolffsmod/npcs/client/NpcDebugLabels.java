package com.wolffsmod.npcs.client;

import com.flansmodultimate.platform.client.ClientPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.npcs.network.DebugLabelPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the label the aim debug switch puts above an NPC: its target, tactic and ranges, one line each, seen through
 * walls. A label the server stops sending fades after a moment. Client side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcDebugLabels
{
    /** Client ticks a label lasts after the server last sent it; the server sends one twice a second. */
    private static final int LIFETIME = 30;
    /** Blocks above the top of the NPC the lowest line floats. */
    private static final double LIFT = 0.6D;
    private static final float SCALE = 0.025F;
    private static final int LINE_HEIGHT = 10;
    private static final int COLOUR = 0xFFFFFFFF;
    private static final int BACKGROUND = 0x60000000;

    private record Label(List<Component> lines, int expires)
    {}

    private static final Map<Integer, Label> labels = new HashMap<>();
    private static int ticks;

    public static void receive(DebugLabelPacket packet)
    {
        labels.put(packet.entityId(), new Label(packet.lines(), ticks + LIFETIME));
    }

    /** At the end of every client tick. */
    public static void tick()
    {
        ticks++;
        if (!labels.isEmpty())
            labels.values().removeIf(label -> label.expires() < ticks);
    }

    public static void clear()
    {
        labels.clear();
    }

    /** Draws the labels, after the world, in the camera-relative pose of the level renderer. */
    public static void render(PoseStack pose, Camera camera, float partialTick)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (labels.isEmpty() || minecraft.level == null)
            return;
        Font font = minecraft.font;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Vec3 eye = camera.getPosition();
        for (Map.Entry<Integer, Label> entry : labels.entrySet())
        {
            Entity npc = minecraft.level.getEntity(entry.getKey());
            if (npc == null)
                continue;
            Vec3 at = npc.getPosition(partialTick).add(0D, npc.getBbHeight() + LIFT, 0D).subtract(eye);
            List<Component> lines = entry.getValue().lines();
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            pose.mulPose(camera.rotation());
            ClientPlatform.scaleCameraFacingLabel(pose, SCALE);
            Matrix4f matrix = pose.last().pose();
            for (int line = 0; line < lines.size(); line++)
            {
                Component text = lines.get(line);
                // The first line sits on top, the last just above the NPC.
                float y = (float) (line - lines.size() + 1) * LINE_HEIGHT;
                font.drawInBatch(text, -font.width(text) / 2F, y, COLOUR, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, BACKGROUND, LightTexture.FULL_BRIGHT);
            }
            pose.popPose();
        }
        buffers.endBatch();
    }
}
