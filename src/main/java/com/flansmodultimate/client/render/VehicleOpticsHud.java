package com.flansmodultimate.client.render;

import com.flansmodultimate.client.gui.OpticsHudEditorScreen;
import com.flansmodultimate.common.driveables.OpticsHud;
import com.flansmodultimate.common.entity.Seat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Scope overlay plus range, speed, traverse, compass and elevation instruments. */
public final class VehicleOpticsHud
{
    private static double range = -1D;
    private static boolean rangeRequested;
    private static long lastRangeTick = Long.MIN_VALUE;
    private static boolean rangeCleared;

    private VehicleOpticsHud() {}

    public static void requestRange() { rangeRequested = true; rangeCleared = false; }
    public static void resetRange() { range = -1D; rangeRequested = false; rangeCleared = true; lastRangeTick = Long.MIN_VALUE; }
    public static void resetSession() { resetRange(); rangeCleared = false; }
    public static void openEditor()
    {
        Seat seat = VehicleOpticsClient.activeSeat();
        if (seat != null && seat.getOpticsHud() != null && seat.getOpticsHud().isEdit())
            Minecraft.getInstance().setScreen(new OpticsHudEditorScreen(seat));
    }

    public static boolean render(GuiGraphics graphics, float partialTick, int width, int height)
    {
        Seat seat = VehicleOpticsClient.activeSeat();
        if (seat == null) return false;
        int sight = seat.getCurrentSight();
        ResourceLocation overlay = VehicleOpticsClient.texture(seat, seat.getOptics().overlay(sight));
        if (overlay != null)
        {
            // The reference draws gunsights over a 4:1 quad; gun scopes use a different layout.
            graphics.blit(overlay, width / 2 - height * 2, 0, height * 4, height, 0F, 0F, 1, 1, 1, 1);
        }
        Minecraft mc = Minecraft.getInstance();
        String channel = (seat.isThermalScoped() ? "FLIR" : "TV") + String.format(Locale.ROOT, "  %.1fx", seat.getScopeZoom());
        graphics.drawString(mc.font, channel, width - mc.font.width(channel) - 8, 8, 0xFFFFFF);
        OpticsHud hud = seat.getOpticsHud();
        if (hud == null || !hud.isEnabled()) return true;

        long tick = seat.level().getGameTime();
        if (rangeRequested || !hud.isRequireRangeKey() && !rangeCleared && tick != lastRangeTick)
        {
            range = measureRange(seat, hud.getMaxRange());
            rangeRequested = false;
            rangeCleared = false;
            lastRangeTick = tick;
        }
        float bearing = Mth.positiveModulo(mc.gameRenderer.getMainCamera().getYRot() + 180F, 360F);
        double speed = seat.getDriveable().getDeltaMovement().horizontalDistance() * 72D;
        String[] values = {
            rangeCleared ? "0" : range < 0 ? "----" : String.format(Locale.ROOT, "%.0f", range),
            String.format(Locale.ROOT, "%.0f km/h", speed),
            String.format(Locale.ROOT, "%.0f°", Mth.wrapDegrees(seat.getViewAimYaw(partialTick))),
            String.format(Locale.ROOT, "%03.0f°", bearing),
            String.format(Locale.ROOT, "%+.0f°", -seat.getViewAimPitch())
        };
        float scale = hud.scale(sight);
        int color = hud.color(sight) | 0xFF000000;
        for (int i = 0; i < OpticsHud.ELEMENTS.length; i++)
        {
            if (i == 4 && !hud.isElevationIndicator()) continue;
            OpticsHud.Element element = hud.getElements()[i];
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2F + element.x(sight) * scale, height / 2F + element.y(sight) * scale, 0F);
            graphics.pose().scale(scale * element.scale(sight), scale * element.scale(sight), 1F);
            ResourceLocation texture = VehicleOpticsClient.texture(seat, element.texture(sight));
            if (texture != null)
                graphics.blit(texture, -element.getWidth() / 2, -element.getHeight() / 2, element.getWidth(), element.getHeight(),
                    0F, 0F, 1, 1, 1, 1);
            graphics.drawCenteredString(mc.font, values[i], 0, -4, color);
            if (i == 2)
            {
                graphics.fill(-40, 10, 41, 11, color);
                int x = Math.round(Mth.wrapDegrees(seat.getViewAimYaw(partialTick)) / 180F * 40F);
                graphics.fill(x, 7, x + 1, 14, color);
            }
            if (i == 3)
            {
                String[] cardinals = {"N", "E", "S", "W"};
                for (int direction = 0; direction < 4; direction++)
                {
                    float offset = Mth.wrapDegrees(direction * 90F - bearing);
                    if (Math.abs(offset) <= 90F)
                        graphics.drawCenteredString(mc.font, cardinals[direction], Math.round(offset), 12, color);
                }
                ResourceLocation marker = VehicleOpticsClient.texture(seat, hud.marker(sight));
                if (marker != null)
                    graphics.blit(marker, hud.getMarkerX() - hud.getMarkerWidth() / 2, hud.getMarkerY(),
                        hud.getMarkerWidth(), hud.getMarkerHeight(), 0F, 0F, 1, 1, 1, 1);
                else graphics.fill(-1, 8, 2, 12, color);
            }
            if (i == 4)
            {
                graphics.fill(12, -40, 13, 41, color);
                int y = Math.round(seat.getViewAimPitch() / 90F * 40F);
                graphics.fill(9, y, 16, y + 1, color);
            }
            graphics.pose().popPose();
        }
        if (hud.isEdit() && mc.screen == null)
            graphics.drawString(mc.font, Component.translatable("gui.flansmodultimate.optics.editor_hint"), 8, height - 30, color);
        return true;
    }

    private static double measureRange(Seat seat, double maximum)
    {
        Minecraft mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 start = camera.getPosition();
        Vec3 direction = new Vec3(camera.getLookVector());
        // Never query or generate unloaded terrain to make an optical measurement.
        double loadedRange = maximum;
        for (double distance = 0D; distance < maximum; distance += 16D)
        {
            var point = net.minecraft.core.BlockPos.containing(start.add(direction.scale(distance)));
            if (!seat.level().hasChunkAt(point)) { loadedRange = distance; break; }
        }
        Vec3 end = start.add(direction.scale(loadedRange));
        HitResult block = seat.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        double nearest = block.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY : start.distanceTo(block.getLocation());
        for (Entity entity : seat.level().getEntities(mc.player, new AABB(start, end).inflate(1D),
            candidate -> candidate.isPickable() && !(candidate instanceof Seat) && candidate != seat.getDriveable()
                && !candidate.isPassengerOfSameVehicle(seat)))
        {
            var hit = entity.getBoundingBox().clip(start, end);
            if (hit.isPresent()) nearest = Math.min(nearest, start.distanceTo(hit.get()));
        }
        return Double.isFinite(nearest) ? nearest : -1D;
    }
}
