package com.flansmodultimate.client.debug;

import com.flansmodultimate.api.parts.IFlanDamageModel;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.*;
import com.flansmodultimate.common.driveables.parts.PartBodies;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.ToolItem;
import com.flansmodultimate.common.raytracing.hits.*;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.config.ModCommonConfig;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Shows the health of the part under the crosshair while debugging or holding a driveable repair tool, and in debug mode also the normal and composite (HEAT) protection on the first part hitbox face
 * under the crosshair: of a driveable, an AA gun, or an entity wearing a driveable's part boxes.
 */
public final class DriveableArmorDebugHud
{
    private static final double AIM_RANGE = 64D;
    /** How far a repair tool shows part health: a little past the reach it repairs at. */
    private static final double REPAIR_AIM_RANGE = 8D;

    private DriveableArmorDebugHud()
    {}

    /** The part face under the crosshair, with the armour that protects it. */
    record Aimed(float time, EnumDriveablePart part, EnumArmorFacing facing, @Nullable ResolvedVehicleArmor armor, @Nullable Health health)
    {}

    record Health(float current, float maximum, boolean destroyed)
    {}

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui)
            return;
        boolean debug = ModClient.isDebug();
        if (!debug && !holdsRepairTool(mc))
            return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 origin = camera.getPosition();
        Vec3 motion = new Vec3(camera.getLookVector()).scale(debug ? AIM_RANGE : REPAIR_AIM_RANGE);
        Aimed closest = nearer(aimedDriveable(mc.level, origin, motion), aimedPartBody(mc.level, origin, motion));
        closest = nearer(closest, aimedAAGun(mc.level.entitiesForRendering(), origin, motion));
        if (closest == null)
            return;

        int y = screenHeight / 2 + 14;
        Health state = closest.health();
        if (state != null && state.maximum() > 0F)
        {
            String health = String.format(Locale.ROOT, "%s HP: %.0f / %.0f", closest.part().getShortName(), Math.max(0F, state.current()), state.maximum());
            graphics.drawString(mc.font, health, (screenWidth - mc.font.width(health)) / 2, y, healthColor(state), true);
            y += 10;
        }
        if (!debug)
            return;

        ResolvedVehicleArmor armor = closest.armor();
        if (armor == null || !armor.isPlateConfigured(closest.part(), closest.facing()))
            return;
        ArmorPlate plate = armor.plate(closest.part(), closest.facing()).authored();
        String label = String.format(Locale.ROOT, "%s %s | Normal armor: %.1f mm", closest.part().getShortName(), closest.facing().name().toLowerCase(Locale.ROOT), plate.thicknessMm());
        graphics.drawString(mc.font, label, (screenWidth - mc.font.width(label)) / 2, y, 0xFFD700, true);
        String composite = String.format(Locale.ROOT, "Composite armor (HEAT): %.1f mm", plate.heatThicknessMm());
        graphics.drawString(mc.font, composite, (screenWidth - mc.font.width(composite)) / 2, y + 10, 0xFFD700, true);
        if (plate.slopeDeg() > 0F)
        {
            float effective = armor.resolveHit(closest.part(), closest.facing(), closest.facing().outwardNormal().scale(-1D), ModCommonConfig.maxArmorImpactAngleDeg()).effectiveArmorMm();
            float effectiveHeat = armor.resolveHit(closest.part(), closest.facing(), closest.facing().outwardNormal().scale(-1D), ModCommonConfig.maxArmorImpactAngleDeg(), true).effectiveArmorMm();
            String slope = String.format(Locale.ROOT, "Slope %.1f° | Head-on normal %.1f mm | HEAT %.1f mm", plate.slopeDeg(), effective, effectiveHeat);
            graphics.drawString(mc.font, slope, (screenWidth - mc.font.width(slope)) / 2, y + 20, 0xFFD700, true);
        }
    }

    private static boolean holdsRepairTool(Minecraft mc)
    {
        for (InteractionHand hand : InteractionHand.values())
        {
            if (mc.player.getItemInHand(hand).getItem() instanceof ToolItem tool && tool.getConfigType().isHealDriveables())
                return true;
        }
        return false;
    }

    /** Green when intact, yellow when damaged, red when destroyed. */
    static int healthColor(Health state)
    {
        if (state.destroyed())
            return 0xFF5555;
        return state.current() < state.maximum() ? 0xFFFF55 : 0x55FF55;
    }

    @Nullable
    private static Health health(@Nullable DriveablePart state)
    {
        return state == null ? null : new Health(state.getHealth(), state.getMaxHealth(), state.isDestroyed());
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
                    closest = nearer(closest, new Aimed(partHit.getIntersectTime(), partHit.getPart(), partHit.getFacing(), driveable.getConfigType().getResolvedArmor(),
                        health(driveable.getDriveableData() == null ? null : driveable.getDriveableData().getPart(partHit.getPart()))));
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
            if (body == null || !(wearer instanceof IFlanDamageModel stand) || !stand.flansmodultimateModelArmour() || !(stand.flansmodultimateDamageModel().orElse(null) instanceof DriveableType))
                continue;
            for (BulletHit hit : body.trace(wearer, origin, motion, true))
            {
                if (hit instanceof PartBodyHit partHit)
                    closest = nearer(closest, new Aimed(partHit.getIntersectTime(), partHit.getPart(), partHit.getFacing(), body.armor(), health(body.partState(partHit.getPart()))));
            }
        }
        return closest;
    }

    /** AA guns have one health pool and no driveable armour plates; trace their actual rotating hull. */
    @Nullable
    static Aimed aimedAAGun(Iterable<Entity> entities, Vec3 origin, Vec3 motion)
    {
        Aimed closest = null;
        for (Entity entity : entities)
        {
            if (!(entity instanceof AAGun aaGun) || aaGun.isRemoved() || aaGun.getConfigType() == null)
                continue;
            Vec3 point = aaGun.clipCollisionBox(origin, origin.add(motion), 0D);
            if (point == null)
                continue;
            float time = motion.lengthSqr() <= 1.0E-8D ? 0F : (float) (origin.distanceTo(point) / motion.length());
            closest = nearer(closest, new Aimed(time, EnumDriveablePart.CORE, EnumArmorFacing.FRONT, null,
                new Health(aaGun.getHealth(), aaGun.getConfigType().getHealth(), aaGun.isWreck() || aaGun.getHealth() <= 0)));
        }
        return closest;
    }
}
