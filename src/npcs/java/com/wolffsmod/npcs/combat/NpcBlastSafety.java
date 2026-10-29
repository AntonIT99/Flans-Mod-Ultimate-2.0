package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.projectiles.DetonationRadii;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDriver;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * How far from its target a vehicle, aircraft or ship has to be before it fires an explosive round at it, so that its own
 * blast does not catch it. The round's reach is read from what the weapon has loaded; the NPC keeps its blast margin
 * beyond that, and no further than most of its firing range, or a weapon whose blast outreaches its range would never
 * fire at all. Whoever fires asks here before it pulls the trigger, and whatever moves the NPC asks before it closes in.
 * Server side.
 *
 * <p>
 * Grenades an NPC throws by hand keep their own distances, which {@link NpcThrowing} works out from the same reach.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcBlastSafety
{
    /** Share of a round's fragment reach whoever fires it keeps out of: fragments that far off seldom hit, and do little. */
    static final double FRAGMENT_SHARE = 0.5D;
    /** Share of its firing range a standoff may take up, so that a weapon whose blast outreaches its range still fires. */
    static final double MAX_RANGE_SHARE = 0.9D;
    /** Ticks between readings of what a weapon has loaded, which scans the driveable for its rounds. */
    public static final int REACH_INTERVAL = 20;

    /** @return blocks round where it goes off within which a round harms, as far as whoever fires it keeps out of it */
    public static double reach(DetonationRadii radii)
    {
        return Math.max(Math.max(radii.explosion(), radii.blast()), Math.max(radii.fire(), radii.fragments() * FRAGMENT_SHARE));
    }

    /** @return that reach for a Flan round, and 0 for another item or one that does not go off */
    public static double reach(ItemStack round)
    {
        return FlansProjectiles.getDetonationRadii(round).map(NpcBlastSafety::reach).orElse(0D);
    }

    /**
     * @param reach
     *            blocks round where it goes off within which the round harms
     * @param margin
     *            blocks the NPC keeps beyond that reach
     * @param range
     *            the NPC's firing range, most of which the standoff may take up at most
     * @return blocks from its target within which the NPC does not fire the round; 0 for one that does not go off
     */
    public static double standoff(double reach, double margin, double range)
    {
        return reach <= 0D ? 0D : Math.min(reach + Math.max(0D, margin), Math.max(0D, range) * MAX_RANGE_SHARE);
    }

    /**
     * @param reach
     *            blocks round where it goes off within which the round harms
     * @return blocks from its target within which the NPC does not fire that round; 0 while it does not keep clear of its
     *         own blast
     */
    public static double standoff(EntityNPCInterface npc, double reach)
    {
        if (!((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.BLAST_STANDOFF))
            return 0D;
        return standoff(reach, NpcTuning.of(npc).get(Value.BLAST_MARGIN), npc.stats.ranged.getRange());
    }

    /**
     * @return how far the round the NPC's main weapon has loaded now reaches: the weapon of the driveable it drives, or its
     *         own model's. An NPC that is neither keeps no standoff; a grenade it throws by hand is {@link NpcThrowing}'s.
     */
    public static double mainWeaponReach(EntityNPCInterface npc)
    {
        return Math.max(NpcDriver.of(npc).blastReach(), NpcVehicleState.of(npc).blastReach());
    }

    /** @return blocks from its target within which the NPC's main weapon would catch it in its own blast */
    public static double standoff(EntityNPCInterface npc)
    {
        return standoff(npc, mainWeaponReach(npc));
    }

    /**
     * @param reach
     *            blocks round where it goes off within which the round harms
     * @return whether the NPC is too close to the target to fire that round without being caught in its blast
     */
    public static boolean tooClose(EntityNPCInterface npc, LivingEntity target, double reach)
    {
        double standoff = standoff(npc, reach);
        return standoff > 0D && npc.distanceTo(target) < standoff;
    }
}
