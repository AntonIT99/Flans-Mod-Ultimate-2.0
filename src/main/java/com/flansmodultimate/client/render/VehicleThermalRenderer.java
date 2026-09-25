package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.platform.client.ClientPlatform;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Depth-tested white-hot FLIR, composed independently of the game's selected post effect. */
public final class VehicleThermalRenderer
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "shaders/post/vehicle_thermal.json");
    private static PostChain chain;
    private static ResourceLocation whiteTexture;
    private static final MultiBufferSource.BufferSource buffers = VertexPlatform.immediateBuffers(256);
    private static boolean renderingMask;
    private static boolean failed;
    private static boolean maskReady;
    private static int width, height;

    private VehicleThermalRenderer() {}
    public static boolean isRenderingMask() { return renderingMask; }

    public static void reset()
    {
        if (chain != null) chain.close();
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        maskReady = false;
        if (!VehicleOpticsClient.thermal())
        {
            if (chain != null) reset();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (failed || mc.level == null) return;
        RenderTarget main = mc.getMainRenderTarget();
        try
        {
            if (chain == null)
            {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, EFFECT);
                chain.resize(main.width, main.height);
                width = main.width; height = main.height;
            }
            if (width != main.width || height != main.height)
            {
                chain.resize(main.width, main.height);
                width = main.width; height = main.height;
            }
            if (whiteTexture == null)
            {
                DynamicTexture texture = new DynamicTexture(1, 1, false);
                texture.getPixels().setPixelRGBA(0, 0, -1);
                texture.upload();
                whiteTexture = mc.getTextureManager().register("vehicle_thermal_white", texture);
            }
            RenderTarget heat = chain.getTempTarget("heat");
            heat.clear(Minecraft.ON_OSX);
            heat.copyDepthFrom(main);
            heat.bindWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderType maskType = RenderType.entitySolid(whiteTexture);
            VertexConsumer mask = new HeatVertexConsumer(buffers.getBuffer(maskType));
            MultiBufferSource maskBuffers = ignored -> mask;
            renderingMask = true;
            Vec3 camera = event.getCamera().getPosition();
            Seat occupied = VehicleOpticsClient.activeSeat();
            for (Entity entity : mc.level.entitiesForRendering())
            {
                if (!(entity instanceof LivingEntity || entity instanceof Driveable) || entity == mc.player
                    || !entity.isAlive() || entity.isInvisible() || occupied != null && entity == occupied.getDriveable()
                    || !event.getFrustum().isVisible(entity.getBoundingBox())) continue;
                renderEntity(entity, camera, ClientPlatform.partialTick(event), event.getPoseStack(), maskBuffers);
            }
            buffers.endBatch();
            renderingMask = false;
            main.bindWrite(false);
            maskReady = true;
        }
        catch (Exception ex)
        {
            if (chain != null) chain.close();
            chain = null;
            failed = true;
            LOGGER.error("Could not render vehicle thermal optics; reload resources to retry", ex);
        }
        finally
        {
            renderingMask = false;
            main.bindWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void composite(float partialTick)
    {
        if (!maskReady || chain == null || !VehicleOpticsClient.thermal()) return;
        maskReady = false;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        try
        {
            chain.process(partialTick);
            // Post passes clear their output; preserve world depth afterwards.
            main.copyDepthFrom(chain.getTempTarget("heat"));
        }
        finally
        {
            main.bindWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }

    private static <E extends Entity> void renderEntity(E entity, Vec3 camera, float partial, PoseStack pose, MultiBufferSource buffer)
    {
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        Vec3 offset = renderer.getRenderOffset(entity, partial);
        pose.pushPose();
        try
        {
            pose.translate(Mth.lerp(partial, entity.xOld, entity.getX()) - camera.x + offset.x,
                Mth.lerp(partial, entity.yOld, entity.getY()) - camera.y + offset.y,
                Mth.lerp(partial, entity.zOld, entity.getZ()) - camera.z + offset.z);
            renderer.render(entity, Mth.rotLerp(partial, entity.yRotO, entity.getYRot()), partial, pose, buffer, LightTexture.FULL_BRIGHT);
        }
        finally { pose.popPose(); }
    }

    private record HeatVertexConsumer(VertexConsumer delegate) implements VertexConsumer
    {
        @Override public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { delegate.color(255, 255, 255, 255); return this; }
        @Override public VertexConsumer uv(float u, float v) { delegate.uv(0.5F, 0.5F); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(OverlayTexture.NO_OVERLAY); return this; }
        @Override public VertexConsumer uv2(int u, int v) { delegate.uv2(LightTexture.FULL_BRIGHT); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override public void endVertex() { delegate.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(255, 255, 255, 255); }
        @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}
