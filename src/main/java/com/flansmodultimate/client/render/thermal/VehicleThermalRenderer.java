package com.flansmodultimate.client.render.thermal;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.distant.DistantBoxRenderer;
import com.flansmodultimate.client.render.WhiteTexture;
import com.flansmodultimate.client.render.hud.VehicleOpticsClient;
import com.flansmodultimate.common.entity.*;
import com.flansmodultimate.mixin.PostChainAccessor;
import com.flansmodultimate.platform.client.ClientPlatform;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.flansmodultimate.util.FlansLog;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Depth-tested white-hot FLIR, composed independently of the game's selected post effect. */
public final class VehicleThermalRenderer
{
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "shaders/post/vehicle_thermal.json");
    private static PostChain chain;
    private static final MultiBufferSource.BufferSource buffers = VertexPlatform.immediateBuffers(256);
    private static boolean renderingMask;
    private static boolean failed;
    private static boolean maskReady;
    private static int width, height;

    private VehicleThermalRenderer()
    {}

    public static boolean isRenderingMask()
    {
        return renderingMask;
    }

    public static void reset()
    {
        if (chain != null)
            chain.close();
        chain = null;
        failed = false;
        maskReady = false;
    }

    public static void render(RenderLevelStageEvent event)
    {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL)
        {
            composite(ClientPlatform.partialTick(event));
            return;
        }
        // AFTER_LEVEL's Forge 1.20.1 pose contains the projection, not the world
        // view. Build the mask here using the same world pose as ordinary entities,
        // then compose at AFTER_LEVEL so weather and the remaining scene are included.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        maskReady = false;
        if (!ThermalVision.active())
        {
            if (chain != null)
                reset();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (failed || mc.level == null)
            return;
        RenderTarget main = mc.getMainRenderTarget();
        try
        {
            if (chain == null)
            {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, EFFECT);
                chain.resize(main.width, main.height);
                width = main.width;
                height = main.height;
            }
            if (width != main.width || height != main.height)
            {
                chain.resize(main.width, main.height);
                width = main.width;
                height = main.height;
            }
            RenderTarget heat = chain.getTempTarget("heat");
            heat.clear(Minecraft.ON_OSX);
            heat.copyDepthFrom(main);
            heat.bindWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderType maskType = RenderType.entitySolid(WhiteTexture.get());
            VertexConsumer mask = new HeatVertexConsumer(buffers.getBuffer(maskType));
            MultiBufferSource maskBuffers = ignored -> mask;
            renderingMask = true;
            Vec3 camera = event.getCamera().getPosition();
            Seat occupied = VehicleOpticsClient.activeSeat();
            float partialTick = ClientPlatform.partialTick(event);
            for (Entity entity : mc.level.entitiesForRendering())
            {
                if (inMask(entity, occupied, event.getFrustum(), partialTick))
                    renderEntity(entity, camera, partialTick, event.getPoseStack(), maskBuffers);
            }
            // Driveables too far away to be drawn as entities are just as hot
            DistantBoxRenderer.renderHeatMask(event.getPoseStack(), mask, camera, ClientPlatform.partialTick(event));
            ThermalHotParticles.renderMask(event.getPoseStack(), mask, camera, event.getCamera().rotation(), event.getFrustum(), partialTick);
            buffers.endBatch();
            renderingMask = false;
            main.bindWrite(false);
            maskReady = true;
        }
        catch (Exception ex)
        {
            if (chain != null)
                chain.close();
            chain = null;
            failed = true;
            FlansLog.log.error("Could not render vehicle thermal optics; reload resources to retry", ex);
        }
        finally
        {
            renderingMask = false;
            main.bindWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void composite(float partialTick)
    {
        if (!maskReady || chain == null || !ThermalVision.active())
            return;
        maskReady = false;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        try
        {
            applyImageSettings();
            chain.process(partialTick);
            // Post passes clear their output; preserve world depth afterwards.
            main.copyDepthFrom(chain.getTempTarget("heat"));
        }
        finally
        {
            main.bindWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }

    /** Feeds the sight's palette and generation, and the clock for the generations' noise, to the thermal pass. */
    private static void applyImageSettings()
    {
        float time = (Util.getMillis() % 1_000_000L) / 1000F;
        float palette = ThermalVision.palette().ordinal();
        float generation = ThermalVision.generation();
        for (PostPass pass : ((PostChainAccessor) chain).flansmodultimatePasses())
        {
            EffectInstance effect = pass.getEffect();
            effect.safeGetUniform("ThermalTime").set(time);
            effect.safeGetUniform("Palette").set(palette);
            effect.safeGetUniform("Generation").set(generation);
        }
    }

    /**
     * Whether an entity is drawn hot: living things and driveables, and since the Labjac Edition flying rounds
     * and grenades, except the viewer, the driveable they look out of, and team-mates between strobe pulses.
     */
    private static boolean inMask(Entity entity, @Nullable Seat occupied, Frustum frustum, float partialTick)
    {
        boolean hot = entity instanceof LivingEntity || entity instanceof Driveable || entity instanceof Bullet || entity instanceof Grenade;
        if (!hot || entity == Minecraft.getInstance().player || !entity.isAlive() || entity.isInvisible())
            return false;
        if (occupied != null && entity == occupied.getDriveable() || !frustum.isVisible(entity.getBoundingBox()))
            return false;
        return !ThermalTeamStrobe.isFriendlyInColdPhase(entity, partialTick);
    }

    private static <E extends Entity> void renderEntity(E entity, Vec3 camera, float partial, PoseStack pose, MultiBufferSource buffer)
    {
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        Vec3 offset = renderer.getRenderOffset(entity, partial);
        pose.pushPose();
        try
        {
            pose.translate(Mth.lerp(partial, entity.xOld, entity.getX()) - camera.x + offset.x, Mth.lerp(partial, entity.yOld, entity.getY()) - camera.y + offset.y,
                Mth.lerp(partial, entity.zOld, entity.getZ()) - camera.z + offset.z);
            renderer.render(entity, Mth.rotLerp(partial, entity.yRotO, entity.getYRot()), partial, pose, buffer, LightTexture.FULL_BRIGHT);
        }
        finally
        {
            pose.popPose();
        }
    }

    private record HeatVertexConsumer(VertexConsumer delegate) implements VertexConsumer
    {
        @Override
        public VertexConsumer vertex(double x, double y, double z)
        {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a)
        {
            delegate.color(255, 255, 255, 255);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v)
        {
            delegate.uv(0.5F, 0.5F);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v)
        {
            delegate.overlayCoords(OverlayTexture.NO_OVERLAY);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v)
        {
            delegate.uv2(LightTexture.FULL_BRIGHT);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z)
        {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex()
        {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a)
        {
            delegate.defaultColor(255, 255, 255, 255);
        }

        @Override
        public void unsetDefaultColor()
        {
            delegate.unsetDefaultColor();
        }
    }
}
