package com.flansmodultimate.client.render.blockentity;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.client.teams.TeamFlagTextures;
import com.flansmodultimate.common.block.TeamBannerBlock;
import com.flansmodultimate.common.block.entity.TeamBannerBlockEntity;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

/**
 * The unused 16x64 banner texture on a two-sided cloth, half a block wide and two blocks tall, in
 * the colour of the team holding the base, under that team's flag image when it has one.
 */
@SuppressWarnings("unused") // Created by the renderer factory registered through Forge's client event bus.
public final class TeamBannerRenderer implements BlockEntityRenderer<TeamBannerBlockEntity>
{
    private static final float EMBLEM_MAX_WIDTH = 0.42F;
    private static final float EMBLEM_MAX_HEIGHT = 0.9F;
    private static final float EMBLEM_TOP_MARGIN = 0.12F;
    /** Distance of the emblem from the cloth, enough to keep the two from flickering. */
    private static final float EMBLEM_OFFSET = 0.004F;

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
        TeamFlagTextures.FlagTexture image = TeamFlagTextures.get(banner.getTeamName().isBlank() ? null : Team.getTeam(banner.getTeamName()));
        if (image != null)
            renderEmblem(image, buffer.getBuffer(RenderType.entityCutoutNoCull(image.location())), pose, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** The team's flag image across the top of the cloth on both faces, undistorted and readable from either side. */
    private static void renderEmblem(TeamFlagTextures.FlagTexture image, VertexConsumer vertices, PoseStack.Pose pose, int packedLight, int packedOverlay)
    {
        float halfWidth = EMBLEM_MAX_WIDTH / 2F;
        float height = EMBLEM_MAX_WIDTH / image.aspect();
        if (height > EMBLEM_MAX_HEIGHT)
        {
            height = EMBLEM_MAX_HEIGHT;
            halfWidth = EMBLEM_MAX_HEIGHT * image.aspect() / 2F;
        }
        float top = -EMBLEM_TOP_MARGIN;
        float bottom = top - height;
        for (int side = -1; side <= 1; side += 2)
        {
            float z = side * EMBLEM_OFFSET;
            float left = side > 0 ? 0F : 1F;
            float right = 1F - left;
            VertexPlatform.vertex(vertices, pose, -halfWidth, top, z, 1F, 1F, 1F, 1F, left, 0F, packedOverlay, packedLight, 0F, 0F, side);
            VertexPlatform.vertex(vertices, pose, -halfWidth, bottom, z, 1F, 1F, 1F, 1F, left, 1F, packedOverlay, packedLight, 0F, 0F, side);
            VertexPlatform.vertex(vertices, pose, halfWidth, bottom, z, 1F, 1F, 1F, 1F, right, 1F, packedOverlay, packedLight, 0F, 0F, side);
            VertexPlatform.vertex(vertices, pose, halfWidth, top, z, 1F, 1F, 1F, 1F, right, 0F, packedOverlay, packedLight, 0F, 0F, side);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull TeamBannerBlockEntity banner)
    {
        // The cloth extends below the attachment block used by the normal culling test.
        return true;
    }
}
