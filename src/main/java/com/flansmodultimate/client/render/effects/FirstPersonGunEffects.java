package com.flansmodultimate.client.render.effects;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.model.ModelMuzzleSmoke;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.client.ClientPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The Tyrants and Plebeians first-person gun effects: drifting chamber smoke, muzzle smoke that stays on the gun, and
 * "fancy" spent casings and en-bloc clips thrown with simple physics. They live in first-person render space and are
 * turned back by the view change since they were fired, so they appear to stay put while the player looks around.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FirstPersonGunEffects
{
    private static final int DETACHED_SMOKE_COUNT = 64;
    private static final int CASING_COUNT = 64;
    /** Casings follow a quarter of the player's movement, as in Tyrants and Plebeians */
    private static final float CASING_FOLLOW = 0.25F;
    private static final RandomSource RANDOM = RandomSource.create();

    private static final GunSmokePuff[] detachedSmoke = createSmoke(DETACHED_SMOKE_COUNT);
    private static final GunCasingParticle[] casings = new GunCasingParticle[CASING_COUNT];
    private static int nextDetachedSmoke;
    private static int nextCasing;

    static
    {
        for (int i = 0; i < casings.length; i++)
            casings[i] = new GunCasingParticle();
    }

    public static GunSmokePuff[] createSmoke(int count)
    {
        GunSmokePuff[] puffs = new GunSmokePuff[count];
        for (int i = 0; i < count; i++)
            puffs[i] = new GunSmokePuff();
        return puffs;
    }

    /** Client tick: moves the detached smoke and casings against the player's own movement. */
    public static void tick(@Nullable LocalPlayer player)
    {
        if (player == null)
            return;

        float dx = (float) (player.xo - player.getX());
        float dy = (float) (player.yo - player.getY());
        float dz = (float) (player.zo - player.getZ());

        for (GunSmokePuff puff : detachedSmoke)
        {
            if (!puff.isAlive())
                continue;
            float yaw = (float) Math.toRadians(puff.getSpawnYaw());
            float localX = dx * Mth.cos(yaw) + dz * Mth.sin(yaw);
            float localZ = dz * Mth.cos(yaw) - dx * Mth.sin(yaw);
            puff.tick(localZ, dy, -localX);
        }

        for (GunCasingParticle casing : casings)
        {
            if (!casing.isAlive())
                continue;
            float yaw = (float) Math.toRadians(casing.getSpawnYaw());
            float pitch = (float) Math.toRadians(casing.getSpawnPitch());
            float localX = dx * Mth.cos(yaw) + dz * Mth.sin(yaw);
            float localZ = dz * Mth.cos(yaw) - dx * Mth.sin(yaw);
            float forward = localZ * Mth.cos(pitch) - dy * Mth.sin(pitch);
            float up = dy * Mth.cos(pitch) + localZ * Mth.sin(pitch);
            casing.tick(forward * CASING_FOLLOW, up * CASING_FOLLOW, -localX * CASING_FOLLOW);
        }
    }

    /** Spawns the gun's chamber puff after a shot, at {@code chamberSmokePoint}, in the gun's unscaled render space. */
    public static void spawnChamberSmoke(ModelGun model)
    {
        GunType type = model.getType();
        ModelMuzzleSmoke smoke = ModelCache.getOrLoadSmokeModel(type, type.getChamberSmokeModelClassName(), type.getChamberSmokeTexture());
        LocalPlayer player = Minecraft.getInstance().player;
        if (!model.isHasChamberSmoke() || smoke == null || player == null || model.getChamberSmokeVanishConst() <= 0F)
            return;

        float modelScale = type.getModelScale();
        Vector3f at = new Vector3f(model.getChamberSmokePoint().x * modelScale, model.getChamberSmokePoint().y * modelScale, model.getChamberSmokePoint().z * modelScale);
        Vector3f random = model.getChamberSmokeRandomVelocity();
        Vector3f velocity = new Vector3f(model.getChamberSmokeVelocity().x + random.x * (RANDOM.nextFloat() - 0.5F),
            model.getChamberSmokeVelocity().y + random.y * (RANDOM.nextFloat() - 0.5F), model.getChamberSmokeVelocity().z + random.z * (RANDOM.nextFloat() - 0.5F));
        float rotation = model.isChamberSmokeFreeRotate() ? RANDOM.nextFloat() * 360F : 0F;
        detachedSmoke[nextDetachedSmoke].spawnDrifting(smoke, type.getChamberSmokeTexture(), model.getChamberSmokeScale(), model.getChamberSmokeScaleEx(), at, velocity, rotation,
            model.getChamberSmokeVanishConst(), player.getYRot(), player.getXRot());
        nextDetachedSmoke = (nextDetachedSmoke + 1) % detachedSmoke.length;
    }

    /** Adds a muzzle puff to the gun's own smoke at {@code muzzlePoint}, in the gun's scaled model space. */
    public static void spawnMuzzleSmoke(ModelGun model, GunAnimations animations, Vector3f muzzlePoint)
    {
        GunType type = model.getType();
        ModelMuzzleSmoke smoke = ModelCache.getOrLoadSmokeModel(type, type.getMuzzleSmokeModelClassName(), type.getMuzzleSmokeTexture());
        if (!model.isHasSmoke() || smoke == null || model.getMuzzleSmokeVanishConst() <= 0F)
            return;

        Vector3f at = new Vector3f(muzzlePoint.x + model.getMuzzleSmokeOffset().x, muzzlePoint.y + model.getMuzzleSmokeOffset().y, muzzlePoint.z + model.getMuzzleSmokeOffset().z);
        float rotation = model.isMuzzleSmokeFreeRotate() ? RANDOM.nextFloat() * 360F : 0F;
        animations.nextMuzzleSmoke().spawn(smoke, type.getMuzzleSmokeTexture(), model.getMuzzleSmokeScale(), model.getMuzzleSmokeScaleEx(), at, rotation, model.getMuzzleSmokeVanishConst());
    }

    /**
     * Throws a spent casing after a shot, and the en-bloc clip with the last round, in the gun's unscaled render space.
     *
     * @param clipTexture texture of the en-bloc clip, which is part of the gun model
     */
    public static void spawnCasing(ModelGun model, GunAnimations animations, int roundsLeft, ResourceLocation clipTexture)
    {
        GunType type = model.getType();
        ModelCasing casingModel = ModelCache.getOrLoadCasingModel(type);
        LocalPlayer player = Minecraft.getInstance().player;
        if (casingModel == null || player == null)
            return;

        float spawnScale = model.getCaseScale() * type.getModelScale();
        float velocityScale = spawnScale * 0.35F / Math.max(1F, model.getCasingAnimTime());
        Vector3f random = animations.getCasingRandom();
        Vector3f at = new Vector3f(model.getCasingAttachPoint().x * spawnScale, model.getCasingAttachPoint().y * spawnScale, model.getCasingAttachPoint().z * spawnScale);
        Vector3f velocity = new Vector3f((model.getCasingAnimDistance().x + random.x * model.getCasingAnimSpread().x) * velocityScale,
            (model.getCasingAnimDistance().y + random.y * model.getCasingAnimSpread().y) * velocityScale, (model.getCasingAnimDistance().z + random.z * model.getCasingAnimSpread().z) * velocityScale);
        Vector3f spin = new Vector3f(model.getCasingRotateVector().x * 18F, model.getCasingRotateVector().y * 18F, model.getCasingRotateVector().z * 18F);
        int life = Math.max(1, (int) model.getCasingLife());

        casings[nextCasing].spawn(casingModel, null, type.getCasingTexture(), at, velocity, spin, model.getCaseScale(), life, player.getYRot(), player.getXRot(), model.getCasingGravity(),
            model.getCasingDrag());
        nextCasing = (nextCasing + 1) % casings.length;
        if (model.isHasEnbloc() && roundsLeft == 0 && model.getEnBlocClipModel().length > 0)
        {
            casings[nextCasing].spawn(null, model.getEnBlocClipModel(), clipTexture, at, velocity, spin, type.getModelScale(), life, player.getYRot(), player.getXRot(), model.getCasingGravity(),
                model.getCasingDrag());
            nextCasing = (nextCasing + 1) % casings.length;
        }
    }

    /** Draws the gun's muzzle smoke in its scaled model space. */
    public static void renderMuzzleSmoke(GunAnimations animations, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        for (GunSmokePuff puff : animations.getMuzzleSmoke())
            renderPuff(puff, false, poseStack, buffer, packedLight);
    }

    /** Draws the drifting chamber smoke in first-person render space. */
    public static void renderDetachedSmoke(PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        for (GunSmokePuff puff : detachedSmoke)
            renderPuff(puff, true, poseStack, buffer, packedLight);
    }

    /** Draws the thrown casings and clips in the gun's unscaled render space. */
    public static void renderCasings(PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        float smoothing = ClientPlatform.partialTick();
        for (GunCasingParticle casing : casings)
        {
            if (!casing.isAlive() || casing.getTexture() == null)
                continue;
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.wrapDegrees(casing.getSpawnYaw() - player.getYRot())));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-(casing.getSpawnPitch() - player.getXRot())));
            translate(poseStack, casing.getLastPosition(), casing.getPosition(), smoothing);
            poseStack.mulPose(Axis.YP.rotationDegrees(casing.getRotation().y));
            poseStack.mulPose(Axis.ZP.rotationDegrees(casing.getRotation().z));
            poseStack.mulPose(Axis.XP.rotationDegrees(casing.getRotation().x));
            poseStack.scale(casing.getScale(), casing.getScale(), casing.getScale());

            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(casing.getTexture()));
            if (casing.getClipModel() != null)
            {
                for (ModelRendererTurbo part : casing.getClipModel())
                    if (part != null)
                        part.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F, 1F, EnumRenderPass.DEFAULT);
            }
            else if (casing.getCasingModel() != null)
            {
                casing.getCasingModel().renderCasing(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F, 1F, EnumRenderPass.DEFAULT);
            }
            poseStack.popPose();
        }
    }

    private static void renderPuff(GunSmokePuff puff, boolean detached, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        ModelMuzzleSmoke model = puff.getModel();
        LocalPlayer player = Minecraft.getInstance().player;
        if (model == null || puff.getTexture() == null || player == null)
            return;

        poseStack.pushPose();
        if (detached)
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-(puff.getSpawnPitch() - player.getXRot())));
            poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.wrapDegrees(puff.getSpawnYaw() - player.getYRot())));
        }
        translate(poseStack, puff.getLastPosition(), puff.getPosition(), ClientPlatform.partialTick());
        poseStack.mulPose(Axis.XP.rotationDegrees(puff.getRotationX()));
        float scale = puff.currentScale();
        poseStack.scale(scale, scale, scale);
        model.renderSmoke(poseStack, buffer.getBuffer(RenderType.entityTranslucent(puff.getTexture())), packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, puff.getAlpha(), 1F,
            EnumRenderPass.DEFAULT);
        poseStack.popPose();
    }

    private static void translate(PoseStack poseStack, Vector3f last, Vector3f now, float smoothing)
    {
        poseStack.translate(last.x + (now.x - last.x) * smoothing, last.y + (now.y - last.y) * smoothing, last.z + (now.z - last.z) * smoothing);
    }
}
