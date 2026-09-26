package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.api.client.FlansModelPreviews;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;

/**
 * Vanilla entity model drawing the static preview of a content-pack AA gun or driveable model.
 *
 * <p>Custom NPCs renders a picked model entity by calling this model from its own NPC renderer, so
 * all drawing happens here rather than in {@link FlanModelRenderer}. Every render pass goes to the
 * single buffer Custom NPCs supplies.</p>
 */
public class FlanModelEntityModel extends EntityModel<FlanModelEntity>
{
    /** The lift {@code LivingEntityRenderer} applies for vanilla models, which hang down from 24 px above the feet. */
    private static final float LIVING_MODEL_ORIGIN = 1.501F;

    private final FlanModelEntityType entityType;

    public FlanModelEntityModel(FlanModelEntityType entityType)
    {
        super(texture -> {
            IContentType type = entityType.getInfoType();
            return type != null ? FlansModelPreviews.getRenderType(type, texture) : RenderType.entityCutoutNoCull(texture);
        });
        this.entityType = entityType;
    }

    @Override
    public void setupAnim(@NotNull FlanModelEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        // Static preview for now.
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int packedColor)
    {
        IContentType type = entityType.getInfoType();
        if (type == null)
            return;

        // The 1.21 model API passes the tint as one ARGB value.
        float alpha = ((packedColor >>> 24) & 0xFF) / 255F;
        float red = ((packedColor >>> 16) & 0xFF) / 255F;
        float green = ((packedColor >>> 8) & 0xFF) / 255F;
        float blue = (packedColor & 0xFF) / 255F;

        poseStack.pushPose();
        // Undo the flipped, lifted frame of vanilla models and stand the model on the NPC's feet.
        poseStack.translate(0F, LIVING_MODEL_ORIGIN, 0F);
        poseStack.scale(-1F, -1F, 1F);
        poseStack.translate(0F, entityType.getShape().modelHeight(), 0F);
        FlansModelPreviews.render(type, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }
}
