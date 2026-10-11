package com.flansmodultimate.client.render.effects;

import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/** Labjac's dark elliptical near-miss vignette, faded independently of wounded and flashbang overlays. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SuppressionOverlay
{
    private static final int SEGMENTS = 96;

    // HUD callbacks include partial ticks; this real-time envelope is already interpolated independently of ticks.
    @SuppressWarnings({"java:S1172", "unused"})
    public static void render(GuiGraphics graphics, float partialTick, int width, int height)
    {
        ModClientConfig config = ModClientConfig.get();
        var player = Minecraft.getInstance().player;
        if (config == null || !config.nearMissSuppression || player == null || !player.isAlive() || player.isSpectator())
        {
            CombatScreenEffects.resetSuppression();
            return;
        }
        if (Minecraft.getInstance().options.hideGui)
            return;
        float level = (float) Math.min(1D, CombatScreenEffects.suppressionLevel(Util.getMillis(), config.suppressionDuration) * config.suppressionIntensity);
        float alpha = (float) (level * config.suppressionVignette);
        if (alpha <= 0.002F)
            return;
        float strength = (float) Math.sqrt(level);
        float radiusX = width * (0.48F - strength * 0.16F);
        float radiusY = height * (0.46F - strength * 0.16F);
        float middleX = Math.min(width * 0.60F, radiusX + width * 0.11F);
        float middleY = Math.min(height * 0.64F, radiusY + height * 0.13F);
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        drawBand(graphics, width, height, radiusX, radiusY, middleX, middleY, 0F, alpha * 0.42F, false);
        drawBand(graphics, width, height, middleX, middleY, 0F, 0F, alpha * 0.42F, alpha, true);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    // The two concentric rings deliberately share one routine and the same angular sampling.
    @SuppressWarnings("java:S107")
    private static void drawBand(GuiGraphics graphics, int width, int height, float innerX, float innerY, float outerX, float outerY, float innerAlpha, float outerAlpha, boolean screenEdge)
    {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        var matrix = graphics.pose().last().pose();
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        for (int i = 0; i <= SEGMENTS; i++)
        {
            double angle = Math.PI * 2D * i / SEGMENTS;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float edge = screenEdge ? Math.min(halfWidth / Math.max(0.0001F, Math.abs(cos)), halfHeight / Math.max(0.0001F, Math.abs(sin))) * 1.01F : 0F;
            float x = screenEdge ? cos * edge : cos * outerX;
            float y = screenEdge ? sin * edge : sin * outerY;
            buffer.vertex(matrix, halfWidth + x, halfHeight + y, 0F).color(0F, 0F, 0F, outerAlpha).endVertex();
            buffer.vertex(matrix, halfWidth + cos * innerX, halfHeight + sin * innerY, 0F).color(0F, 0F, 0F, innerAlpha).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
    }
}
