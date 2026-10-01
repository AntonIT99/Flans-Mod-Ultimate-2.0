package com.wolffsmod.npcs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.layer.LayerInterface;
import noppes.npcs.client.layer.LayerPreRender;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.shared.client.model.Model2DRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Matches Custom NPCs' headwear behavior with the legacy skin height. */
@SuppressWarnings("rawtypes")
public final class LegacyHeadwearLayer64x32 extends LayerInterface implements LayerPreRender
{
    private final LegacyHeadwear64x32 headwear = new LegacyHeadwear64x32();

    public LegacyHeadwearLayer64x32(LivingEntityRenderer renderer)
    {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (CustomNpcs.HeadWearType != 1 || this.npc.textureLocation == null)
            return;

        float red = 1F;
        float green = 1F;
        float blue = 1F;
        if (this.npc.hurtTime <= 0 && this.npc.deathTime <= 0)
        {
            int tint = this.npc.display.getTint();
            red = (tint >> 16 & 0xFF) / 255F;
            green = (tint >> 8 & 0xFF) / 255F;
            blue = (tint & 0xFF) / 255F;
        }

        this.base.head.translateAndRotate(poseStack);
        Model2DRenderer.textureOverride = this.npc.textureLocation;
        try
        {
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(this.npc.textureLocation));
            this.headwear.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY,
                red, green, blue, this.alpha());
        }
        finally
        {
            Model2DRenderer.textureOverride = null;
        }
    }

    @Override
    public void rotate(PoseStack poseStack, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
    {
    }

    @Override
    public void preRender(EntityCustomNpc npc)
    {
        this.base.hat.visible = this.base.head.visible && CustomNpcs.HeadWearType != 1;
        if (!this.base.hat.visible)
            this.headwear.config = null;
    }
}
