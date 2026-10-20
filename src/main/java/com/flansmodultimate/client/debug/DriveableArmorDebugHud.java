package com.flansmodultimate.client.debug;

import com.flansmodultimate.api.IFlanDamageModel;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.*;
import com.flansmodultimate.common.driveables.parts.PartBodies;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.raytracing.hits.*;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.config.ModCommonConfig;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Shows normal and composite (HEAT) protection on the first part hitbox face under the crosshair: of a driveable, or of an entity
 * wearing a driveable's part boxes.
 */
public final class DriveableArmorDebugHud
{
    private static final double AIM_RANGE = 64D;

    private DriveableArmorDebugHud()
    {}

    /** The part face under the crosshair, with the armour that protects it. */
    private record Aimed(float time, EnumDriveablePart part, EnumArmorFacing facing, ResolvedVehicleArmor armor)
    {}

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight)
    {
        Minecraft mc = Minecraft.getInstance();
        if (!ModClient.isDebug() || mc.level == null || mc.player == null || mc.options.hideGui)
            return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 origin = camera.getPosition();
        Vec3 motion = new Vec3(camera.getLookVector()).scale(AIM_RANGE);
        Aimed closest = nearer(aimedDriveable(mc.level, origin, motion), aimedPartBody(mc.level, origin, motion));
        if (closest == null)
            return;

        ResolvedVehicleArmor armor = closest.armor();
        if (!armor.isPlateConfigured(closest.part(), closest.facing()))
            return;
        ArmorPlate plate = armor.plate(closest.part(), closest.facing()).authored();
        String label = String.format(Locale.ROOT, "%s %s | Normal armor: %.1f mm", closest.part().getShortName(), closest.facing().name().toLowerCase(Locale.ROOT), plate.thicknessMm());
        graphics.drawString(mc.font, label, (screenWidth - mc.font.width(label)) / 2, screenHeight / 2 + 14, 0xFFD700, true);
        String composite = String.format(Locale.ROOT, "Composite armor (HEAT): %.1f mm", plate.heatThicknessMm());
        graphics.drawString(mc.font, composite, (screenWidth - mc.font.width(composite)) / 2, screenHeight / 2 + 24, 0xFFD700, true);
        if (plate.slopeDeg() > 0F)
        {
            float effective = armor.resolveHit(closest.part(), closest.facing(), closest.facing().outwardNormal().scale(-1D), ModCommonConfig.maxArmorImpactAngleDeg()).effectiveArmorMm();
            float effectiveHeat = armor.resolveHit(closest.part(), closest.facing(), closest.facing().outwardNormal().scale(-1D), ModCommonConfig.maxArmorImpactAngleDeg(), true).effectiveArmorMm();
            String slope = String.format(Locale.ROOT, "Slope %.1f° | Head-on normal %.1f mm | HEAT %.1f mm", plate.slopeDeg(), effective, effectiveHeat);
            graphics.drawString(mc.font, slope, (screenWidth - mc.font.width(slope)) / 2, screenHeight / 2 + 34, 0xFFD700, true);
        }
    }

    @Nullable
    private static Aimed nearer(@Nullable Aimed first, @Nullable Aimed second)
    {
        if (first == null || second == null)
            return first == null ? second : first;
        return second.time() < first.time() ? second : first;
    }

    @Nullable
    private static Aimed aimedDriveable(ClientLevel level, Vec3 origin, Vec3 motion)
    {
        Aimed closest = null;
        for (Entity entity : level.entitiesForRendering())
        {
            if (!(entity instanceof Driveable driveable) || driveable.getConfigType() == null)
                continue;
            for (BulletHit hit : driveable.attackFromBullet(origin, motion, true))
            {
                if (hit instanceof DriveableHit partHit)
                    closest = nearer(closest, new Aimed(partHit.getIntersectTime(), partHit.getPart(), partHit.getFacing(), driveable.getConfigType().getResolvedArmor()));
            }
        }
        return closest;
    }

    /** An entity hit by the part shows the armour rounds really meet: none while its model armour is switched off. */
    @Nullable
    private static Aimed aimedPartBody(ClientLevel level, Vec3 origin, Vec3 motion)
    {
        Aimed closest = null;
        for (Entity wearer : PartBodies.wearers(level))
        {
            PartBody body = PartBodies.of(wearer);
            if (body == null || !(wearer instanceof IFlanDamageModel stand) || !stand.flansmodultimateModelArmour()
                || !(stand.flansmodultimateDamageModel().orElse(null) instanceof DriveableType type))
                continue;
            for (BulletHit hit : body.trace(wearer, origin, motion, true))
            {
                if (hit instanceof PartBodyHit partHit)
                    closest = nearer(closest, new Aimed(partHit.getIntersectTime(), partHit.getPart(), partHit.getFacing(), type.getResolvedArmor()));
            }
        }
        return closest;
    }
}
