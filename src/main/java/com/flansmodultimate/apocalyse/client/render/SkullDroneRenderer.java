package com.flansmodultimate.apocalyse.client.render;

import com.flansmod.client.model.ModelGun;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.common.entity.SkullDroneEntity;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.item.GunItemRenderer;
import com.flansmodultimate.common.item.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The legacy skull, four counter-rotating propellers, and a mounted gun that pitches towards its target. */
public final class SkullDroneRenderer extends EntityRenderer<SkullDroneEntity>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "textures/entity/skulldrone.png");
    private static final List<Vec3> PROPELLER_ORIGINS = List.of(new Vec3(1.1D, 0.55D, 1.1D), new Vec3(1.1D, 0.55D, -1.1D), new Vec3(-1.1D, 0.55D, -1.1D), new Vec3(-1.1D, 0.55D, 1.1D));

    private final ModelPart body;
    private final ModelPart propeller;
    private final ItemRenderer itemRenderer;

    public SkullDroneRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        body = createBodyLayer().bakeRoot();
        propeller = createPropellerLayer().bakeRoot();
        itemRenderer = context.getItemRenderer();
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 0F, -4F, 8F, 8F, 8F).texOffs(32, 0).addBox(-3F, -4F, -3F, 6F, 4F, 6F), PartPose.ZERO);
        for (int i = 0; i < PROPELLER_ORIGINS.size(); i++)
        {
            Vec3 origin = PROPELLER_ORIGINS.get(i);
            root.addOrReplaceChild("arm" + i, CubeListBuilder.create().texOffs(56, 0).addBox(-1F, 0F, -1F, 2F, 24F, 2F),
                PartPose.offsetAndRotation((float) origin.x * 4F, 0F, (float) origin.z * 4F, -Mth.PI * 0.375F, Mth.PI * (1.25F + i * 0.5F), 0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createPropellerLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("blades", CubeListBuilder.create().texOffs(0, 16).addBox(-6F, -0.5F, -0.5F, 12F, 1F, 1F).texOffs(0, 18).addBox(-0.5F, -0.5F, -6F, 1F, 1F, 12F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(@NotNull SkullDroneEntity entity, float yaw, float partialTick, @NotNull PoseStack pose, @NotNull MultiBufferSource buffer, int light)
    {
        pose.pushPose();
        // Legacy models point along +X, whereas Minecraft yaw zero points along +Z.
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) - 90F));
        body.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        for (int i = 0; i < PROPELLER_ORIGINS.size(); i++)
        {
            Vec3 origin = PROPELLER_ORIGINS.get(i);
            pose.pushPose();
            pose.translate(origin.x, origin.y, origin.z);
            pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * (i % 2 == 0 ? -80F : 80F)));
            pose.scale(2F, 2F, 2F);
            propeller.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        renderWeapon(entity, partialTick, pose, buffer, light);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    private void renderWeapon(SkullDroneEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffer, int light)
    {
        ItemStack stack = entity.getMainHandItem();
        if (stack.isEmpty())
            return;
        pose.pushPose();
        pose.translate(0D, -0.5D, 0D);
        pose.mulPose(Axis.ZP.rotationDegrees(-Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        if (stack.getItem() instanceof GunItem gun && ModelCache.getOrLoadTypeModel(gun.getConfigType()) instanceof ModelGun model)
        {
            GunItemRenderer.renderEmbedded(model, stack, ModClient.getGunAnimations(entity, InteractionHand.MAIN_HAND), pose, buffer, light, OverlayTexture.NO_OVERLAY);
        }
        else
        {
            pose.mulPose(Axis.ZP.rotationDegrees(-135F));
            pose.translate(0D, -0.4D, 0D);
            itemRenderer.renderStatic(entity, stack, ItemDisplayContext.NONE, false, pose, buffer, entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId());
        }
        pose.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SkullDroneEntity entity)
    {
        return TEXTURE;
    }
}
