package com.flansmodultimate.apocalyse.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.common.entity.SkullBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The built-in skull boss's target-facing head, laughing jaw and two-turn drone-spawn cue. */
public final class SkullBossRenderer extends EntityRenderer<SkullBossEntity>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "textures/entity/skullboss.png");
    private final ModelPart head;
    private final ModelPart jaw;

    @SuppressWarnings("unused") // Forge calls the renderer factory through its client registration event.
    public SkullBossRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 0F, -4F, 8F, 8F, 8F), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(32, 0).addBox(-3F, -4F, -3F, 6F, 4F, 6F), PartPose.ZERO);
        ModelPart root = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        head = root.getChild("head");
        jaw = root.getChild("jaw");
    }

    @Override
    public void render(@NotNull SkullBossEntity entity, float yaw, float partialTick, @NotNull PoseStack pose, @NotNull MultiBufferSource buffer, int light)
    {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) + entity.getSpawnSpin(partialTick)));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        pose.scale(32F, 32F, 32F);
        head.zRot = entity.getLaughFactor(partialTick) * 15F * Mth.DEG_TO_RAD;
        jaw.zRot = -head.zRot;
        var vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        head.render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
        jaw.render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SkullBossEntity entity)
    {
        return TEXTURE;
    }
}
