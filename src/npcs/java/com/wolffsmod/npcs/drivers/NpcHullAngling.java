package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.driveables.ISeat;
import com.wolffsmod.npcs.sim.MountMotion;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Turns a halted tracked vehicle's front armour toward the most dangerous enemy ground vehicle nearby, angled a little off
 * it so that its plates slope, rather than leaving its side to it. The turret keeps aiming at the target meanwhile.
 */
public final class NpcHullAngling
{
    /** Degrees off the threat the hull is held at, to slope its front plates. */
    static final float ANGLE = 25F;
    /** Degrees off the threat beyond which the hull starts turning toward it. */
    static final float START = 40F;
    /** Degrees within the angled facing at which a turn stops. */
    static final float SETTLE = 5F;
    private static final double MIN_RANGE = 48D;
    private static final int SCAN_INTERVAL = 10;

    @Nullable
    private UUID threat;
    private long scannedAt = Long.MIN_VALUE / 2;
    private boolean turning;

    /**
     * @param off
     *            degrees from the hull's heading to the threat, positive to the right
     * @param turning
     *            whether the hull was turning last tick
     * @return degrees to turn the hull, positive to the right, or 0 to hold it
     */
    static float course(float off, boolean turning)
    {
        float wrapped = Mth.wrapDegrees(off);
        float magnitude = Math.abs(wrapped);
        float error = magnitude - ANGLE;
        if (Math.abs(error) <= (turning ? SETTLE : START - ANGLE) && magnitude <= START)
            return 0F;
        return Math.signum(wrapped) * error;
    }

    /**
     * @return degrees to turn a halted vehicle to show its front to a threat, positive to the right, or null without one
     */
    @Nullable
    public Float turn(EntityNPCInterface npc, Entity driveable, Vec3 heading)
    {
        Entity danger = threat(npc, driveable);
        if (danger == null)
        {
            turning = false;
            return null;
        }
        float off = Mth.wrapDegrees(MountMotion.yaw(danger.position().subtract(driveable.position())) - MountMotion.yaw(heading));
        float course = course(off, turning);
        turning = course != 0F;
        return course;
    }

    public void reset()
    {
        threat = null;
        turning = false;
        scannedAt = Long.MIN_VALUE / 2;
    }

    @Nullable
    private Entity threat(EntityNPCInterface npc, Entity driveable)
    {
        LivingEntity target = npc.getTarget();
        if (target != null && target.isAlive() && target.level() == npc.level() && NpcVehicleThreats.groundVehicle(target))
        {
            Entity platform = NpcVehicleThreats.platform(target);
            if (platform != driveable)
                return platform;
        }
        long now = npc.level().getGameTime();
        double range = Math.max(MIN_RANGE, npc.stats.ranged.getRange());
        if (now - scannedAt >= SCAN_INTERVAL)
        {
            scannedAt = now;
            threat = null;
            double best = Double.MAX_VALUE;
            for (Entity other : npc.level().getEntities(driveable, driveable.getBoundingBox().inflate(range), other -> other.isAlive() && other != npc))
            {
                if (NpcVehicleThreats.platform(other) != other || !NpcVehicleThreats.groundVehicle(other) || !hostile(npc, other))
                    continue;
                double distance = other.distanceToSqr(driveable);
                if (distance < best)
                {
                    best = distance;
                    threat = other.getUUID();
                }
            }
        }
        if (threat == null || !(npc.level() instanceof ServerLevel level))
            return null;
        Entity found = level.getEntity(threat);
        if (found == null || !found.isAlive() || found.distanceToSqr(driveable) > range * range)
        {
            threat = null;
            return null;
        }
        return found;
    }

    /** Whether a vehicle is crewed by an enemy of the NPC, or is an enemy NPC model itself. */
    private static boolean hostile(EntityNPCInterface npc, Entity vehicle)
    {
        if (vehicle instanceof EntityNPCInterface)
            return NpcDrivers.hostile(npc, vehicle);
        for (Entity passenger : vehicle.getIndirectPassengers())
        {
            Entity rider = passenger instanceof ISeat seat ? seat.getRiddenByEntity() : passenger;
            if (rider != null && rider != npc && NpcDrivers.hostile(npc, rider))
                return true;
        }
        return false;
    }
}
