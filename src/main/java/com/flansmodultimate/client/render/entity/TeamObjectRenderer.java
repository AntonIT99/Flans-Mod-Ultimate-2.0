package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Flag;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Port of the legacy ModelFlagpole: the three-block pole and the flag that flies from it. */
public final class TeamObjectRenderer<T extends Entity> extends EntityRenderer<T>
{
    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 32;
    private static final float RIGHT_ANGLE = Mth.HALF_PI;
    /** Degrees per tick a dropped flag turns on the spot. */
    private static final float CARRIED_SPIN_SPEED = 2F;

    private final ModelPart pole;
    private final ModelPart flag;

    public TeamObjectRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.25F;
        ModelPart root = createLayer().bakeRoot();
        pole = root.getChild("pole");
        flag = root.getChild("flag");
    }

    /** Legacy box list, kept in its original texture offsets on the 64x32 sheet. */
    private static LayerDefinition createLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition poleRoot = root.addOrReplaceChild("pole", CubeListBuilder.create(), PartPose.ZERO);
        poleRoot.addOrReplaceChild("upper", CubeListBuilder.create().texOffs(0, 16).addBox(-48F, -1F, -1F, 24, 2, 2),
            PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, RIGHT_ANGLE));
        poleRoot.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(0, 16).addBox(-24F, -1F, -1F, 24, 2, 2),
            PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, RIGHT_ANGLE));
        poleRoot.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 20).addBox(-2F, -2F, -2F, 4, 2, 4), PartPose.ZERO);

        root.addOrReplaceChild("flag", CubeListBuilder.create().texOffs(0, 0).addBox(-8F, -16F, 0F, 16, 16, 0),
            PartPose.offset(8F, 0F, 0F));

        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void render(@NotNull T entity, float yaw, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight)
    {
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(FlansMod.TEXTURE_FLAGPOLE));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        if (entity instanceof Flag flagEntity)
        {
            // A flag on its pole hangs from the top; a dropped or carried one turns on the spot.
            // Legacy lifted it by half a block because the flag rode the pole entity; here the
            // flag entity already sits at the top of the pole, so it hangs straight down from there.
            if (!flagEntity.isHome())
            {
                poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * CARRIED_SPIN_SPEED));
                poseStack.translate(0.5F, 0F, 0F);
            }

            int colour = flagEntity.getColour();
            poseStack.scale(-1F, -1F, 1F);
            flag.render(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY,
                (colour >> 16 & 255) / 255F, (colour >> 8 & 255) / 255F, (colour & 255) / 255F, 1F);
        }
        else
        {
            poseStack.scale(-1F, -1F, 1F);
            pole.render(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(@NotNull T entity)
    {
        return FlansMod.TEXTURE_FLAGPOLE;
    }
}
