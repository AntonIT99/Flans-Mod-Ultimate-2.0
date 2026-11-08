package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.WheelSupport;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Holds an NPC wearing a driveable model to the ground a player-driven one of that definition would be held to.
 *
 * <p>
 * The NPC collides through its own mob box, which is about a block wide and sits under the middle of the model. A tank
 * whose box stands on the corner of a roof is carried by that one block with its whole hull in the air, where a real
 * driveable, which rests on the terrain under its wheels, would never have stayed. The wheel sample says how much of
 * the footprint is actually carried; too little of it, and the hull is tipped off the way it hangs over, until either
 * the wheels find ground again or nothing is left under it and it falls.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcWheelSupport
{
    /** Share of its wheels a model keeps on the ground to be carried by them. Below half the footprint, the hull tips. */
    private static final double HOLD_SHARE = 0.5D;
    /** Horizontal acceleration, in blocks per tick squared, with which an overhanging hull slides off its support. */
    private static final double TOPPLE_ACCELERATION = 0.045D;
    /** Fastest, in blocks per tick, that sliding alone pushes a hull off its support. */
    private static final double TOPPLE_SPEED = 0.3D;
    /** Downward velocity, in blocks per tick, a hull with nothing under its wheels is given to leave the ledge. */
    private static final double DROP_SPEED = 0.1D;

    /**
     * Tips an overhanging model off whatever carries its mob box. Server side, once per tick, after the wheels were
     * sampled.
     *
     * @param npc
     *            the NPC wearing the model
     * @param support
     *            what that tick's wheel sample found; an unusable sample leaves the NPC alone
     * @param facing
     *            the model's hull yaw in degrees, used only when every wheel hangs and there is no way to tell which
     *            side it hangs over
     */
    public static void topple(EntityNPCInterface npc, WheelSupport support, float facing)
    {
        if (npc.isPassenger() || npc.isInWater() || npc.isKilled() || !support.overhanging(HOLD_SHARE))
            return;
        Vec3 direction = slideDirection(support, facing);
        Vec3 motion = npc.getDeltaMovement();
        // The share of the footprint left hanging decides how hard it goes over: a hull on its last wheel barely holds.
        double push = TOPPLE_ACCELERATION * (1D - support.share() / HOLD_SHARE);
        double x = motion.x + direction.x * push;
        double z = motion.z + direction.z * push;
        double speed = Math.sqrt(x * x + z * z);
        if (speed > TOPPLE_SPEED)
        {
            x = x / speed * TOPPLE_SPEED;
            z = z / speed * TOPPLE_SPEED;
        }
        // With no wheel at all on the ground the mob box is the only thing holding it up, and it has to come off that too.
        double y = support.unsupported() ? Math.min(motion.y, -DROP_SPEED) : motion.y;
        npc.setDeltaMovement(x, y, z);
        npc.hasImpulse = true;
    }

    /** @return the horizontal unit vector the hull goes over along: towards the wheels that hang, or its nose when all of them do */
    private static Vec3 slideDirection(WheelSupport support, float facing)
    {
        Vec3 overhang = support.overhang();
        double length = overhang.horizontalDistance();
        if (length > 1.0E-4D)
            return new Vec3(overhang.x / length, 0D, overhang.z / length);
        float radians = (float) Math.toRadians(facing);
        return new Vec3(-Mth.sin(radians), 0D, Mth.cos(radians));
    }
}
