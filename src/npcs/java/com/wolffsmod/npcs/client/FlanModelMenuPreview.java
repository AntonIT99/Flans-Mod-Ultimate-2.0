package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.client.FlansModelPreviews;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.wolffsmod.npcs.model.FlanModelEntity;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import org.joml.Quaternionf;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** Static model-menu preview; its framing never changes the NPC's saved size or world hitbox. */
public final class FlanModelMenuPreview
{
    private FlanModelMenuPreview() {}

    public static void render(GuiCreationScreenInterface screen, GuiGraphics graphics, FlanModelEntity model, int rotation)
    {
        var type = model.getInfoType();
        var modelType = model.getModelType();
        if (type == null || modelType == null)
            return;
        float lift = modelType.getShape().modelHeight();
        AABB bounds = FlanModelBounds.of(model);
        ResourceLocation texture = screen.playerdata.simpleRender ? model.getModelTexture()
            : ResourceLocation.tryParse(screen.npc.display.getSkinTexture());
        if (texture == null)
            texture = model.getModelTexture();
        if (texture == null)
            return;
        int left = screen.guiLeft + 128, right = screen.guiLeft + screen.imageWidth - 12;
        int top = screen.guiTop + 32, bottom = screen.guiTop + 194;
        Quaternionf orientation = Axis.XP.rotationDegrees(20).mul(Axis.YP.rotationDegrees(rotation));
        float scale = FlanPreviewFraming.pixelsPerBlock(bounds, orientation, right - left, bottom - top);
        var center = bounds.getCenter();
        graphics.flush();
        graphics.enableScissor(left, top, right, bottom);
        var pose = graphics.pose();
        pose.pushPose();
        try
        {
            pose.translate((left + right) / 2F, (top + bottom) / 2F, 100);
            pose.scale(scale, -scale, scale);
            pose.mulPose(orientation);
            pose.translate(-center.x, -center.y + lift, -center.z);
            Lighting.setupForEntityInInventory();
            var vertices = graphics.bufferSource().getBuffer(FlansModelPreviews.getRenderType(type, texture));
            FlansModelPreviews.render(type, pose, vertices, 0xF000F0, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            graphics.flush();
        }
        finally
        {
            Lighting.setupFor3DItems();
            pose.popPose();
            graphics.disableScissor();
        }
    }
}
