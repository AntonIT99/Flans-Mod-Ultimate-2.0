package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.common.entity.ThrownGun;
import com.flansmodultimate.common.types.GunType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a thrown weapon with its flat item sprite, tip first. Handheld sprites are drawn from the
 * bottom-left corner to the top-right one, so the sprite is turned 45 degrees to lie along the
 * flight path. {@code ModelScale} sets its length: a scale of 1 spans one block edge.
 */
public class ThrownGunRenderer extends EntityRenderer<ThrownGun>
{
    /** How far behind the entity the sprite's centre sits, in sprite widths, so only the tip sinks into a block */
    private static final float SPRITE_SETBACK = 0.5F;

    private final ItemRenderer itemRenderer;

    public ThrownGunRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
        itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(@NotNull ThrownGun entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        ItemStack weapon = entity.getWeapon();
        if (!weapon.isEmpty())
        {
            GunType gunType = entity.getGunType();
            float scale = gunType != null ? gunType.getModelScale() : 1F;

            poseStack.pushPose();
            // After these two rotations +X points along the flight path, as for arrows and tridents
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
            poseStack.translate(-SPRITE_SETBACK * scale, 0F, 0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-45F));
            // Flat items are always a sixteenth of a block thick; thin HD sprites to their own pixel size
            int spriteWidth = itemRenderer.getModel(weapon, entity.level(), null, entity.getId()).getParticleIcon().contents().width();
            poseStack.scale(scale, scale, scale * Math.min(1F, 16F / Math.max(1, spriteWidth)));
            itemRenderer.renderStatic(weapon, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull ThrownGun entity)
    {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
