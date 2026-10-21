package com.flansmodultimate.client.render.blockentity;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.common.block.TeamBannerBlock;
import com.flansmodultimate.common.block.entity.TeamBannerBlockEntity;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

/** The unused 16x64 banner texture on a two-sided cloth, half a block wide and two blocks tall. */
@SuppressWarnings("unused") // Created by the renderer factory registered through Forge's client event bus.
public final class TeamBannerRenderer implements BlockEntityRenderer<TeamBannerBlockEntity>
{
    @Override
    public void render(@NotNull TeamBannerBlockEntity banner, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        poseStack.pushPose();
        poseStack.translate(0.5D, 1D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-banner.getBlockState().getValue(TeamBannerBlock.FACING).toYRot()));
        poseStack.translate(0D, 0D, -0.4375D);
        int colour = banner.getTeamColour();
        float red = (colour >> 16 & 255) / 255F;
        float green = (colour >> 8 & 255) / 255F;
        float blue = (colour & 255) / 255F;
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(FlansModTextures.TEXTURE_BANNER));
        VertexPlatform.vertex(vertices, pose, -0.25F, 0F, 0F, red, green, blue, 1F, 0F, 0F, packedOverlay, packedLight, 0F, 0F, 1F);
        VertexPlatform.vertex(vertices, pose, -0.25F, -2F, 0F, red, green, blue, 1F, 0F, 1F, packedOverlay, packedLight, 0F, 0F, 1F);
        VertexPlatform.vertex(vertices, pose, 0.25F, -2F, 0F, red, green, blue, 1F, 1F, 1F, packedOverlay, packedLight, 0F, 0F, 1F);
        VertexPlatform.vertex(vertices, pose, 0.25F, 0F, 0F, red, green, blue, 1F, 1F, 0F, packedOverlay, packedLight, 0F, 0F, 1F);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull TeamBannerBlockEntity banner)
    {
        // The cloth extends below the attachment block used by the normal culling test.
        return true;
    }
}
