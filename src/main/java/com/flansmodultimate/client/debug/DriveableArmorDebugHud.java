package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.common.driveables.armor.ResolvedVehicleArmor;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.raytracing.hits.DriveableHit;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Shows the authored nominal plate on the first driveable hitbox face under the crosshair. */
public final class DriveableArmorDebugHud
{
    private static final double AIM_RANGE = 64D;

    private DriveableArmorDebugHud() {}

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight)
    {
        Minecraft mc = Minecraft.getInstance();
        if (!ModClient.isDebug() || mc.level == null || mc.player == null || mc.options.hideGui)
            return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 origin = camera.getPosition();
        Vec3 motion = new Vec3(camera.getLookVector()).scale(AIM_RANGE);
        DriveableHit closest = null;
        for (Entity entity : mc.level.entitiesForRendering())
        {
            if (!(entity instanceof Driveable driveable) || driveable.getConfigType() == null)
                continue;
            for (var hit : driveable.attackFromBullet(origin, motion, true))
            {
                if (hit instanceof DriveableHit partHit
                    && (closest == null || partHit.getIntersectTime() < closest.getIntersectTime()))
                    closest = partHit;
            }
        }
        if (closest == null)
            return;

        ResolvedVehicleArmor armor = closest.getDriveable().getConfigType().getResolvedArmor();
        if (!armor.isPlateConfigured(closest.getPart(), closest.getFacing()))
            return;
        float thickness = armor.plate(closest.getPart(), closest.getFacing()).authored().thicknessMm();
        String label = String.format(Locale.ROOT, "%s %s: %.1f mm",
            closest.getPart().getShortName(), closest.getFacing().name().toLowerCase(Locale.ROOT),
            thickness);
        graphics.drawString(mc.font, label, (screenWidth - mc.font.width(label)) / 2,
            screenHeight / 2 + 14, 0xFFD700, true);
    }
}
