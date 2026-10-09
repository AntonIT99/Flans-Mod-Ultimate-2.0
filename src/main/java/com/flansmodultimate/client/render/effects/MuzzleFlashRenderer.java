package com.flansmodultimate.client.render.effects;

import com.flansmod.client.model.*;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansModTextures;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.render.ShaderPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Shared client preference and rendering for held, deployed and driveable gun flashes. */
public final class MuzzleFlashRenderer
{
    private static final ModelFlash DEFAULT_FLASH = new ModelDefaultFlash();
    private static final ModelMuzzleFlash DEFAULT_MUZZLE_FLASH = new ModelDefaultMuzzleFlash();

    private MuzzleFlashRenderer()
    {}

    @Nullable
    public static ModelBase select(@Nullable GunType gun, boolean allowDefault)
    {
        ModelFlash flash = gun != null && StringUtils.isNotBlank(gun.getFlashModelClassName()) ? ModelCache.getOrLoadFlashModel(gun) : null;
        ModelMuzzleFlash muzzleFlash = gun != null && StringUtils.isNotBlank(gun.getMuzzleFlashModelClassName()) ? ModelCache.getOrLoadMuzzleFlashModel(gun) : null;
        if (allowDefault && flash == null && muzzleFlash == null)
        {
            flash = DEFAULT_FLASH;
            muzzleFlash = DEFAULT_MUZZLE_FLASH;
        }
        return ModClientConfig.get().muzzleFlashStyle.select(flash, muzzleFlash);
    }

    /**
     * With FlashModel declared, both styles use its flash-scaled position convention.
     * MuzzleFlashModel-only legacy packs retain their unscaled model coordinates.
     */
    public static Vector3f muzzlePosition(ModelGun model, @Nullable ModelAttachment barrel, boolean flashCoordinates)
    {
        Vector3f fallback = flashCoordinates ? Vector3f.Zero : Objects.requireNonNullElse(model.getBarrelAttachPoint(), Vector3f.Zero);
        Vector3f point = Objects.requireNonNullElse(model.getMuzzleFlashPoint(), fallback);
        if (point.equals(ModelGun.getInvalid()))
            point = Objects.requireNonNullElse(model.getBarrelAttachPoint(), Vector3f.Zero);
        if (barrel != null)
            point = barrel.getMuzzleFlashPoint(point, model.getBarrelAttachPoint());
        else
            point = Vector3f.add(point, Objects.requireNonNullElse(model.getDefaultBarrelFlashPoint(), Vector3f.Zero), null);
        float scale = flashCoordinates ? model.getFlashScale() : 1F;
        return new Vector3f(point.x * scale, point.y * scale, point.z * scale);
    }

    /**
     * Pose is already at the physical muzzle. The 1.12.2 model keeps its historical fixed size.
     * A flash is light, not an occluder, so it is left out of a shader pack's shadow map.
     */
    public static void render(ModelBase model, @Nullable GunType gun, int frame, float flashScale, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay)
    {
        if (ShaderPlatform.isRenderingShadowPass())
            return;
        if (model instanceof ModelFlash flash)
        {
            ResourceLocation texture = model == DEFAULT_FLASH ? FlansModTextures.TEXTURE_DEFAULTFLASH : gun.getFlashTexture();
            flash.renderFlash(Math.max(0, Math.min(2, frame)), poseStack, buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(texture)), LightTexture.FULL_BRIGHT, packedOverlay, 1F, 1F, 1F, 1F,
                flashScale);
        }
        else if (model instanceof ModelMuzzleFlash muzzleFlash)
        {
            muzzleFlash.renderToBuffer(poseStack, buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(muzzleFlash.getTexture())), LightTexture.FULL_BRIGHT, packedOverlay, 1F, 1F, 1F, 1F);
        }
    }
}
