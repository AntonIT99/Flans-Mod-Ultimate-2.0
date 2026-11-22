package com.flansmodultimate.client.render.hud;

import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.client.HudOverlayPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * The Labjac fork's fighter head-up display, for aircraft seats marked {@code hasHUD}: a heading tape, a pitch ladder that
 * stays on the horizon as the aircraft banks, a bank scale, and speed and altitude tapes either side of the sight, all in
 * the seat's {@code hudColorR/G/B} colour. Laid out in the fork's units, 353 to the screen height, and read from the same
 * composed view the camera renders with, so the ladder sits on the real horizon.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FighterHud
{
    /** Layout units per degree of pitch on the ladder. */
    private static final float LADDER_UNITS_PER_DEGREE = 5F;
    /** Layout units per degree of heading on the tape. */
    private static final float HEADING_UNITS_PER_DEGREE = 6F;
    /** Layout units per unit of speed or block of altitude on the side tapes. */
    private static final float TAPE_UNITS = 2F;
    private static final int ALPHA = 0xCC000000;

    public static final HudOverlayPlatform.HudLayer LAYER = FighterHud::render;

    private static void render(GuiGraphics g, float partialTick, int width, int height)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.options.getCameraType() != CameraType.FIRST_PERSON || mc.getCameraEntity() != player
            || !(player.getVehicle() instanceof Seat seat) || !(seat.getDriveable() instanceof Plane plane))
            return;
        SeatInfo info = seat.getSeatInfo();
        if (info == null || !info.isHasHud() || VehicleOpticsClient.activeSeat() == seat)
            return;
        LegacyDriveableCoordinates.ViewAngles view = MountedCameraView.resolve(player, partialTick);
        if (view == null)
            return;
        int bright = ALPHA | (info.getHudColor() & 0xFFFFFF);
        int dim = ALPHA | dimmed(info.getHudColor());
        Font font = mc.font;
        float heading = Mth.positiveModulo(view.yaw() + 180F, 360F);
        float elevation = -view.pitch();
        double speed = ModClientConfig.get().driveableSpeedUnit.convert(plane.getDeltaMovement().length() * 20D);
        double altitude = plane.getY() - plane.level().getSeaLevel();

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(width / 2F, height / 2F, 0F);
        float scale = height / 353F * 0.8F;
        pose.scale(scale, scale, 1F);

        drawSight(g, bright);
        drawLadder(g, font, elevation, -view.roll(), bright);
        drawBankScale(g, -view.roll(), dim);
        drawHeadingTape(g, font, heading, dim, bright);
        drawTape(g, font, speed, -100, true, dim, bright);
        drawTape(g, font, altitude, 100, false, dim, bright);
        String speedText = String.format(Locale.ROOT, "%.0f", speed);
        drawBoxedValue(g, font, speedText, -140, bright);
        String throttle = Math.round(plane.getThrottle() * 100F) + "%";
        g.drawString(font, throttle, -140 - font.width(throttle) / 2, 10, bright, false);
        drawBoxedValue(g, font, String.format(Locale.ROOT, "%.0f", altitude), 140, bright);
        pose.popPose();
    }

    private static int dimmed(int rgb)
    {
        int r = (int) (((rgb >> 16) & 0xFF) * 0.85F);
        int gr = (int) (((rgb >> 8) & 0xFF) * 0.85F);
        int b = (int) ((rgb & 0xFF) * 0.85F);
        return (r << 16) | (gr << 8) | b;
    }

    /** The fixed gun cross in the middle. */
    private static void drawSight(GuiGraphics g, int color)
    {
        g.fill(-1, -9, 1, -3, color);
        g.fill(-1, 3, 1, 9, color);
        g.fill(-9, -1, -3, 1, color);
        g.fill(3, -1, 9, 1, color);
    }

    /** Rungs every ten degrees and a broken horizon line, turned with the bank so they stay level with the world. */
    private static void drawLadder(GuiGraphics g, Font font, float elevation, float bank, int color)
    {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(bank));
        int lowest = Mth.floor((elevation - 20F) / 5F) * 5;
        for (int value = lowest; value <= elevation + 20F; value += 5)
        {
            int y = Math.round((elevation - value) * LADDER_UNITS_PER_DEGREE);
            if (Math.abs(y) > 100)
                continue;
            if (value == 0)
            {
                g.fill(-93, y - 1, -13, y + 1, color);
                g.fill(13, y - 1, 93, y + 1, color);
            }
            else if (value % 10 == 0)
            {
                g.fill(-40, y, -10, y + 1, color);
                g.fill(10, y, 40, y + 1, color);
                String label = Integer.toString(value);
                g.drawString(font, label, 44, y - 4, color, false);
                g.drawString(font, label, -44 - font.width(label), y - 4, color, false);
            }
        }
        pose.popPose();
    }

    /** Ticks every thirty degrees of bank around the sight, and a pointer that turns with the bank. */
    private static void drawBankScale(GuiGraphics g, float bank, int color)
    {
        PoseStack pose = g.pose();
        for (int angle = -90; angle <= 90; angle += 30)
        {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(angle));
            g.fill(0, 40, 1, 50, color);
            pose.popPose();
        }
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(bank));
        g.fill(-1, 34, 1, 40, color);
        pose.popPose();
    }

    /** The heading tape across the top, with the cardinal points drawn large. */
    private static void drawHeadingTape(GuiGraphics g, Font font, float heading, int tick, int text)
    {
        PoseStack pose = g.pose();
        g.fill(-1, -128, 1, -120, text);
        int first = Mth.floor((heading - 20F) / 5F) * 5;
        for (int value = first; value <= heading + 20F; value += 5)
        {
            int x = Math.round((value - heading) * HEADING_UNITS_PER_DEGREE);
            g.fill(x, -138, x + 1, -128, tick);
            int bearing = Math.floorMod(value, 360);
            String label = switch (bearing)
            {
                case 0 -> "N";
                case 90 -> "E";
                case 180 -> "S";
                case 270 -> "W";
                default -> bearing % 10 == 0 ? Integer.toString(bearing) : null;
            };
            if (label == null)
                continue;
            boolean cardinal = bearing % 90 == 0;
            pose.pushPose();
            pose.translate(x, cardinal ? -158F : -148F, 0F);
            if (cardinal)
                pose.scale(2F, 2F, 1F);
            g.drawString(font, label, -font.width(label) / 2, 0, text, false);
            pose.popPose();
        }
    }

    /** A vertical tape of ticks every ten units with their values, higher values above, on one side of the sight. */
    private static void drawTape(GuiGraphics g, Font font, double value, int x, boolean left, int tick, int text)
    {
        double first = Math.floor((value - 25D) / 10D) * 10D;
        for (double mark = first; mark <= value + 25D; mark += 10D)
        {
            if (left && mark < 0D)
                continue;
            int y = (int) Math.round((value - mark) * TAPE_UNITS);
            g.fill(left ? x - 10 : x, y, left ? x : x + 10, y + 1, tick);
            String label = Long.toString(Math.round(mark));
            g.drawString(font, label, left ? x - 14 - font.width(label) : x + 14, y - 4, text, false);
        }
    }

    /** The current value in a box at the centre of a tape. */
    private static void drawBoxedValue(GuiGraphics g, Font font, String text, int centre, int color)
    {
        int half = font.width(text) / 2 + 3;
        g.fill(centre - half, -7, centre + half, -6, color);
        g.fill(centre - half, 6, centre + half, 7, color);
        g.fill(centre - half, -7, centre - half + 1, 7, color);
        g.fill(centre + half - 1, -7, centre + half, 7, color);
        g.drawString(font, text, centre - font.width(text) / 2, -4, color, false);
    }
}
