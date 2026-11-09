package com.wolffsmod.npcs.crew;

import com.wolffsmod.npcs.sim.*;
import com.wolffsmod.npcs.teams.SquadTransports;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Crew who get out of their vehicle NPC to fight its target on foot, as their seats say. A vehicle on the ground lets them
 * out beside it once it has closed with a target on the ground, or, since it often fights from further off than that, once
 * it has been in action against one for a while and stands still enough for them to get out safely. An aircraft high above
 * the ground drops those who carry a parachute over its target, one at a time, as a stick of paratroopers; the others stay
 * aboard until it is on the ground. Each takes the vehicle's target as its own. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CrewDisembark
{
    /** Ticks between checks, and so between two paratroopers jumping from an aircraft. */
    private static final int INTERVAL = 10;
    private static final int TICKS_PER_SECOND = 20;
    /** Blocks from its target within which a vehicle on the ground lets its crew out at once. */
    private static final double GROUND_RANGE = 32D;
    /** Blocks per tick under which a vehicle fighting from further off stands still enough for its crew to get out. */
    private static final double DEPLOY_SPEED = 0.1D;
    /** Blocks across the ground from its target within which an aircraft drops its paratroopers. */
    private static final double DROP_RADIUS = 32D;
    /** Blocks above the ground an aircraft has to be for its crew to jump, and the most a target may be above it to be fought on foot. */
    private static final double JUMP_HEIGHT = 8D;

    /** Lets out the crew who fight on foot once the vehicle is where they can. Runs on the vehicle's server tick. */
    public static void tick(EntityNPCInterface vehicle)
    {
        if (vehicle.tickCount % INTERVAL != 0 || vehicle.level().isClientSide || vehicle.isKilled() || !vehicle.isAlive())
            return;
        NpcVehicleState state = NpcVehicleState.of(vehicle);
        // A crippled aircraft going down is abandoned while there is height left to jump.
        if (state.getFlight().isCrashing())
        {
            if (height(vehicle) > JUMP_HEIGHT)
                NpcCrew.abandonFalling(vehicle);
            return;
        }
        LivingEntity target = vehicle.getTarget();
        // A target in the air, such as an aircraft the vehicle fights, is none for soldiers on foot.
        boolean onFoot = target != null && target.isAlive() && target.level() == vehicle.level() && height(target) <= JUMP_HEIGHT;
        state.getContact().tick(vehicle.tickCount, onFoot);
        if (!onFoot)
            return;
        List<EntityNPCInterface> leaving = leaving(vehicle);
        if (leaving.isEmpty())
            return;
        if (height(vehicle) > JUMP_HEIGHT)
        {
            if (across(vehicle, target) <= DROP_RADIUS)
                leaving.stream().filter(NpcParachutes::carries).findFirst().ifPresent(member -> getOut(vehicle, member, target));
        }
        else if (vehicle.onGround()
            && deploys(vehicle.distanceTo(target), state.getContact().heldFor(vehicle.tickCount), vehicle.getDeltaMovement().horizontalDistance(), delay(vehicle)))
            leaving.forEach(member -> getOut(vehicle, member, target));
    }

    /** @return ticks the vehicle fights a target further off than it closes to before it puts the crew it carries down */
    private static int delay(EntityNPCInterface vehicle)
    {
        return NpcTuning.of(vehicle).get(Value.DISEMBARK_DELAY) * TICKS_PER_SECOND;
    }

    /**
     * @param distance
     *            blocks between the vehicle and its target
     * @param contact
     *            ticks the vehicle has been fighting it
     * @param speed
     *            blocks per tick the vehicle is travelling across the ground
     * @param delay
     *            ticks of contact the vehicle waits for before it puts the crew down at that distance
     * @return whether a vehicle on the ground lets the crew who fight on foot out: it has closed with its target, or it has
     *         been in action against one further off for long enough and is standing still enough for them to get out safely
     */
    static boolean deploys(double distance, int contact, double speed, int delay)
    {
        if (distance <= GROUND_RANGE)
            return true;
        return contact >= delay && speed <= DEPLOY_SPEED;
    }

    /** @return the living crew aboard whose seats let them out to fight, in seat order */
    private static List<EntityNPCInterface> leaving(EntityNPCInterface vehicle)
    {
        NpcCrewSettings settings = NpcCrewSettings.of(vehicle);
        return NpcCrew.crew(vehicle).stream().filter(member -> member.isAlive() && !member.isKilled() && !SquadTransports.carries(member, vehicle))
            .filter(member -> NpcCrew.seatOf(vehicle, member).map(mount -> settings.disembarks(mount.seat(), CrewPost.of(mount))).orElse(false)).toList();
    }

    private static void getOut(EntityNPCInterface vehicle, EntityNPCInterface member, LivingEntity target)
    {
        NpcCrew.bailOut(vehicle, member);
        member.setTarget(target);
    }

    /** @return blocks between two entities across the ground */
    private static double across(Entity from, Entity to)
    {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** @return blocks the entity is above the ground under it */
    private static double height(Entity entity)
    {
        return entity.getY() - entity.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, entity.getBlockX(), entity.getBlockZ());
    }
}
