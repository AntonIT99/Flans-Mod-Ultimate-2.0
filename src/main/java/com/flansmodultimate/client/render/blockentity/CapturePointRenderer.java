package com.flansmodultimate.client.render.blockentity;

import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.client.render.entity.TeamObjectRenderer;
import com.flansmodultimate.client.teams.TeamFlagTextures;
import com.flansmodultimate.common.block.entity.CapturePointBlockEntity;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a capture point as the Teams flagpole with a flag that climbs the pole as the point is
 * taken, in the colour of the team taking or holding it, under the letter and name of the point.
 */
public final class CapturePointRenderer implements BlockEntityRenderer<CapturePointBlockEntity>
{
    private static final int NEUTRAL_COLOUR = 0xB8B8B8;
    /** Height of the bottom edge of the flag on a neutral point and on one fully held. */
    private static final float FLAG_LOW = 0.25F;
    private static final float FLAG_HIGH = 2F;
    private static final double LABEL_RANGE_SQUARED = 48D * 48D;
    private static final float LABEL_HEIGHT = 3.6F;
    private static final float LABEL_SCALE = 0.026F;
    /** As far as the legacy capture point was drawn. */
    private static final int VIEW_DISTANCE = 140;

    private final ModelPart pole;
    private final ModelPart flag;
    private final Font font;
    private final BlockEntityRenderDispatcher dispatcher;

    public CapturePointRenderer(BlockEntityRendererProvider.Context context)
    {
        ModelPart root = TeamObjectRenderer.createLayer().bakeRoot();
        pole = root.getChild("pole");
        flag = root.getChild("flag");
        font = context.getFont();
        dispatcher = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(@NotNull CapturePointBlockEntity point, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        Team team = point.getDisplayTeam().isBlank() ? null : Team.getTeam(point.getDisplayTeam());
        float lift = Mth.lerp(point.getClientProgress(), FLAG_LOW, FLAG_HIGH);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-point.getYaw()));

        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(FlansModTextures.TEXTURE_FLAGPOLE));
        poseStack.pushPose();
        poseStack.scale(-1F, -1F, 1F);
        pole.render(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.translate(0F, lift, 0F);
        TeamFlagTextures.FlagTexture image = TeamFlagTextures.get(team);
        if (image != null)
            TeamObjectRenderer.renderFlagImage(image, poseStack, buffer, packedLight);
        else
        {
            int colour = team == null ? NEUTRAL_COLOUR : team.getTeamColour();
            poseStack.scale(-1F, -1F, 1F);
            VertexPlatform.renderModelPart(flag, poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, (colour >> 16 & 255) / 255F, (colour >> 8 & 255) / 255F, (colour & 255) / 255F, 1F);
        }
        poseStack.popPose();

        renderLabel(point, poseStack, buffer, packedLight);
    }

    private void renderLabel(CapturePointBlockEntity point, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        Vec3 centre = Vec3.atBottomCenterOf(point.getBlockPos());
        if (dispatcher.camera == null || dispatcher.camera.getPosition().distanceToSqr(centre) > LABEL_RANGE_SQUARED)
            return;
        poseStack.pushPose();
        poseStack.translate(0.5F, LABEL_HEIGHT, 0.5F);
        poseStack.mulPose(dispatcher.camera.rotation());
        // Minecraft 1.21 turns the camera rotation half a turn further than 1.20.1, so the text keeps its X axis, as name tags do.
        poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
        Matrix4f pose = poseStack.last().pose();
        String symbol = String.valueOf(point.getSymbol());
        font.drawInBatch(symbol, -font.width(symbol) / 2F, -10F, 0xFFFFFFFF, false, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0x70000000, packedLight);
        font.drawInBatch(point.getBaseName(), -font.width(point.getBaseName()) / 2F, 0F, 0xFFE0E0E0, false, pose, buffer, Font.DisplayMode.SEE_THROUGH, 0x70000000, packedLight);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull CapturePointBlockEntity point)
    {
        // The pole and its label reach well above the block the culling test knows about.
        return true;
    }

    @Override
    public int getViewDistance()
    {
        return VIEW_DISTANCE;
    }
}
