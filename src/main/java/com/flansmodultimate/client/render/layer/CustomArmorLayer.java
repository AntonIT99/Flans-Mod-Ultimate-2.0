package com.flansmodultimate.client.render.layer;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.api.equipment.IEquipmentPolicy;
import com.flansmodultimate.client.model.EquipmentAnimationSupport;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.entity.DriveableRenderer;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.client.render.preview.WorldModelPreview;
import com.flansmodultimate.client.render.thermal.VehicleThermalRenderer;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.render.ShaderPlatform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the Flan armour a living entity wears.
 *
 * <p>
 * In an ordinary world pass the pieces go through the batched model cache, as driveable and gun geometry does: each body
 * part is one rigid pose, so a uniform costs a handful of palette entries instead of a CPU transform per box per frame,
 * which is what a battle of a few hundred soldiers spends its frame time on. Menus, heat masks and anything drawing
 * through a buffer of its own keep the exact renderer. Boxes too small to see are left out by
 * {@link ArmorLodPolicy}; the complete model is always drawn in a menu, where there is no world camera to measure it
 * against.
 * </p>
 */
public class CustomArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M>
{
    /**
     * The camera projection's scale in the last ordinary world pass. A shader pack's shadow pass replaces the projection
     * with the shadow map's, which says nothing about on-screen size, so that pass reuses the view's own scale.
     */
    private static float viewProjectionPixels;

    public CustomArmorLayer(RenderLayerParent<T, M> parent)
    {
        super(parent);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, @NotNull T entity, float limbSwing, float limbSwingAmount, float partialTicks,
        float ageInTicks, float netHeadYaw, float headPitch)
    {
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0F);
        boolean world = isWorldPass(buffer);
        float minimumPartPixels = beginCulling(entity, partialTicks, world);
        try
        {
            renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.HEAD, packedLight, overlay, world);
            renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.LEGS, packedLight, overlay, world);
            renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.FEET, packedLight, overlay, world);
            renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.CHEST, packedLight, overlay, world);
        }
        finally
        {
            if (minimumPartPixels > 0F)
                ModelRendererTurbo.endScreenSpaceCulling();
        }
    }

    /**
     * Whether this is an ordinary world pass, which the level-of-detail measurements and the batch both need: a menu
     * preview supplies no world camera, and a heat silhouette or an outline draws through a buffer of its own.
     */
    private static boolean isWorldPass(MultiBufferSource buffer)
    {
        return !DriveableRenderer.isRenderingPreview() && !VehicleThermalRenderer.isRenderingMask() && buffer.getClass() == MultiBufferSource.BufferSource.class
            && (ShaderPlatform.isRenderingShadowPass() || WorldModelPreview.perspectiveProjection(RenderSystem.getProjectionMatrix()));
    }

    /**
     * Starts projected-size culling for this wearer.
     *
     * @return the threshold in use, or 0 when nothing is culled and no scope was opened
     */
    // The projection scale of the world pass is shared by every armour layer.
    @SuppressWarnings("java:S2696")
    private static float beginCulling(LivingEntity entity, float partialTicks, boolean world)
    {
        if (!world)
            return 0F;
        boolean shadowPass = ShaderPlatform.isRenderingShadowPass();
        float projectionPixels = viewProjectionPixels;
        if (!shadowPass)
        {
            projectionPixels = Math.abs(RenderSystem.getProjectionMatrix().m11()) * Minecraft.getInstance().getWindow().getHeight() * 0.5F;
            viewProjectionPixels = projectionPixels;
        }
        ModClientConfig config = ModClientConfig.get();
        if (config == null || projectionPixels <= 0F)
            return 0F;
        float minimumPartPixels = (float) config.minimumArmorPartPixelSize;
        double cameraDistance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(entity.getPosition(partialTicks));
        if (config.enableArmorLod)
        {
            minimumPartPixels = ArmorLodPolicy.partThreshold(minimumPartPixels, (float) config.maximumArmorLodPartPixelSize, (float) config.armorLodDetailMultiplier, cameraDistance);
        }
        if (minimumPartPixels <= 0F)
            return 0F;
        if (shadowPass)
        {
            // Depths in the shadow view are measured from the sun. Size every box as the camera sees the wearer
            // instead, so the shadow drops no box the view keeps.
            ModelRendererTurbo.beginFixedScaleCulling(minimumPartPixels, (float) (projectionPixels / Math.max(0.5D, cameraDistance)));
        }
        else
            ModelRendererTurbo.beginScreenSpaceCulling(minimumPartPixels, projectionPixels);
        return minimumPartPixels;
    }

    @SuppressWarnings("unchecked")
    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, T entity, EquipmentSlot slot, int packedLight, int overlay, boolean world)
    {
        ItemStack itemStack = entity.getItemBySlot(slot);
        Item item = itemStack.getItem();

        if (item instanceof CustomArmorItem armorItem && armorItem.getEquipmentSlot() == slot && ModelCache.getOrLoadTypeModel(armorItem.getConfigType()) instanceof ModelCustomArmour model)
        {
            ArmorType armorType = armorItem.getConfigType();
            ResourceLocation texture = armorType.getTexture();
            getParentModel().copyPropertiesTo((HumanoidModel<T>) model);
            if (entity instanceof IEquipmentPolicy policy && !policy.flansArmorAnimations())
                EquipmentAnimationSupport.restArmor(model);
            EquipmentAnimationSupport.armorPoseSteps(model, entity);

            setModelPartVisibility(model, slot, entity);

            boolean translucent = ModClientConfig.get().useTranslucentRendering(armorType);
            boolean cull = ModClientConfig.get().useCullingRendering(armorType);
            for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
            {
                VertexConsumer consumer = GpuModelCache.begin(buffer, renderPass, texture, translucent, cull, world, true);
                try
                {
                    model.renderToBuffer(poseStack, consumer, packedLight, overlay, 1F, 1F, 1F, 1F, renderPass);
                }
                finally
                {
                    GpuModelCache.end(consumer);
                }
            }
        }
    }

    private void setModelPartVisibility(ModelCustomArmour model, EquipmentSlot slot, LivingEntity entity)
    {
        model.setAllVisible(false);

        switch (slot)
        {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = entity instanceof Player player && player.isModelPartShown(PlayerModelPart.HAT);
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
                // no-op
            }
        }
    }
}
