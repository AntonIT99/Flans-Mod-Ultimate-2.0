package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;

/**
 * How far off a hull running under the water is noticed. A submarine at depth is hidden from everything looking across
 * the water, so it is picked up only well inside the range the same hull would be seen at on the surface, and the deeper
 * it runs the closer it has to come before anything notices it. That is what makes a submarine worth taking down, and
 * what gives a submarine something to gain by diving. Nothing else is hidden by being in water: a swimming soldier is
 * seen as it always was. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSubmergedContact
{
    /** Blocks of the searching NPC's range lost for every block of depth the submarine runs at. */
    static final double RANGE_PER_DEPTH = 8D;
    /** Blocks within which a submerged hull is noticed however deep it runs: close enough to be run into. */
    static final double MIN_CONTACT = 12D;

    /** @return whether the searching NPC notices the candidate; only a submerged ship is ever hidden by this */
    public static boolean detectable(EntityNPCInterface npc, LivingEntity candidate)
    {
        return within(npc, candidate, NpcTargetSearch.range(npc));
    }

    /**
     * @return whether the NPC still holds the submerged target it has. Contact is kept out to the margin an NPC keeps any
     *         target to, so that a submarine lying on the edge of contact is not picked up and dropped tick after tick.
     */
    public static boolean holds(EntityNPCInterface npc, LivingEntity target)
    {
        return within(npc, target, NpcTargetSearch.range(npc) + NpcTargetSearch.margin(npc));
    }

    private static boolean within(EntityNPCInterface npc, LivingEntity candidate, double surfaceRange)
    {
        int depth = submergence(candidate);
        if (depth <= 0 || !NpcEquipment.enabled(npc, Feature.SUBMARINE_TRIM))
            return true;
        double range = contactRange(surfaceRange, depth);
        return npc.distanceToSqr(candidate) <= range * range;
    }

    /**
     * @param range
     *            blocks the searching NPC would notice the hull at on the surface
     * @return blocks it notices it at while it runs that deep
     */
    static double contactRange(double range, int depth)
    {
        return Math.max(MIN_CONTACT, range - depth * RANGE_PER_DEPTH);
    }

    /** @return blocks below the surface the candidate is holding as a submarine, or 0 for anything that is not one */
    static int submergence(LivingEntity candidate)
    {
        if (!(candidate instanceof NpcVehicleStateHolder holder))
            return 0;
        NpcVehicleState state = holder.wolffsmodnpcsVehicleState();
        return state == null ? 0 : state.getSubmarine().heldDepth();
    }
}
