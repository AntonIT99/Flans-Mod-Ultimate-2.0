package com.wolffsmod.npcs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.layer.LayerInterface;
import noppes.npcs.client.layer.LayerPreRender;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.shared.client.model.Model2DRenderer;

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
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (CustomNpcs.HeadWearType != 1 || npc.textureLocation == null)
            return;

        float red = 1F;
        float green = 1F;
        float blue = 1F;
        if (npc.hurtTime <= 0 && npc.deathTime <= 0)
        {
            int tint = npc.display.getTint();
            red = (tint >> 16 & 0xFF) / 255F;
            green = (tint >> 8 & 0xFF) / 255F;
            blue = (tint & 0xFF) / 255F;
        }

        base.head.translateAndRotate(poseStack);
        setTextureOverride(npc.textureLocation);
        try
        {
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(npc.textureLocation));
            headwear.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, red, green, blue, alpha());
        }
        finally
        {
            setTextureOverride(null);
        }
    }

    private static void setTextureOverride(ResourceLocation textureLocation)
    {
        Model2DRenderer.textureOverride = textureLocation;
    }

    @Override
    public void rotate(PoseStack poseStack, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
    {
        // no-op
    }

    @Override
    public void preRender(EntityCustomNpc npc)
    {
        base.hat.visible = base.head.visible && CustomNpcs.HeadWearType != 1;
        if (!base.hat.visible)
            headwear.config = null;
    }
}
