package com.wolffsmod.npcs.diag;

import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcActivity;
import com.wolffsmod.npcs.sim.NpcDormancy;
import com.wolffsmod.npcs.sim.NpcRepairs;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.sim.NpcVehicleStateHolder;
import com.wolffsmod.npcs.tactics.NpcSafetyState;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcThreats;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * What the General tab of the Wolff's Mod tab shows of an NPC as it changes, and that only the server knows: what it is
 * doing, at whom, and with what. Its health and the hit points of its parts reach the client by their own sync and are not
 * repeated here.
 *
 * @param health
 *            the NPC's hit points, which the client's copy of a Custom NPC does not keep
 * @param maxHealth
 *            the most hit points the NPC has
 * @param target
 *            the name of the entity the NPC fights, or null
 * @param targetDistance
 *            blocks to the target, or NaN without one
 * @param threat
 *            the name of an enemy shooting at the NPC other than its target, or null
 * @param crewAboard
 *            riders in a seat of the model; {@link #NO_CREW} for an NPC without seats
 * @param crewSeats
 *            seats of the model; {@link #NO_CREW} for an NPC without seats
 * @param respawnSeconds
 *            seconds until a killed NPC comes back, 0 while it waits for the time of day it respawns at, or {@link #NEVER}
 * @param homeDistance
 *            blocks to the place the NPC was put down, its home
 * @param speed
 *            blocks per second the NPC moved in the last tick
 */
public record NpcStatus(Activity activity, float health, float maxHealth, Behaviour behaviour, @Nullable Component target, float targetDistance, boolean targetInSight, @Nullable Component threat,
    boolean reloading, boolean outOfAmmunition, int crewAboard, int crewSeats, boolean repairing, int respawnSeconds, float homeDistance, float speed)
{
    public static final int NO_CREW = -1;
    public static final int NEVER = -1;
    /** Custom NPCs' respawn choices that bring a killed NPC back: always, by day and by night. */
    private static final int LAST_RESPAWNING_CYCLE = 2;
    private static final float TICKS_PER_SECOND = 20F;
    private static final long MILLIS_PER_SECOND = 1000L;

    /** Whether the NPC is running: killed, frozen with every NPC, asleep far from players, or awake. */
    public enum Activity
    {
        AWAKE, HELD, DORMANT, FROZEN, KILLED
    }

    /** What the NPC is doing in a fight, from the move its tactics chose; idle without a target. */
    public enum Behaviour
    {
        IDLE, ENGAGING, FIRING, EVADING, RETREATING, IN_COVER, PEEKING, FLANKING, SPREADING, ADVANCING
    }

    /** @return what the NPC is doing now. Server thread. */
    public static NpcStatus of(EntityNPCInterface npc)
    {
        long now = npc.level().getGameTime();
        NpcTacticState tactic = NpcTacticState.of(npc);
        LivingEntity target = npc.getTarget();
        LivingEntity threat = NpcThreats.threat(npc);
        int seats = NpcCrew.seats(npc).size();
        BlockPos home = npc.ais.startPos();
        double dx = npc.getX() - npc.xo;
        double dy = npc.getY() - npc.yo;
        double dz = npc.getZ() - npc.zo;
        return new NpcStatus(activity(npc), Math.max(0F, npc.getHealth()), npc.getMaxHealth(), behaviour(tactic.getSafety(), target != null, now), target == null ? null : target.getName(),
            target == null ? Float.NaN : (float) Math.sqrt(npc.distanceToSqr(target)), target != null && npc.getSensing().hasLineOfSight(target),
            threat == null || threat == target ? null : threat.getName(), tactic.isReloading(now), outOfAmmunition(npc), seats > 0 ? NpcCrew.seating(npc).size() : NO_CREW,
            seats > 0 ? seats : NO_CREW, NpcRepairs.active(npc), respawnSeconds(npc), (float) Math.sqrt(home.distToCenterSqr(npc.position())),
            npc.isKilled() ? 0F : (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * TICKS_PER_SECOND);
    }

    private static Activity activity(EntityNPCInterface npc)
    {
        if (npc.isKilled())
            return Activity.KILLED;
        if (CustomNpcsPlatform.frozen())
            return Activity.FROZEN;
        if (NpcDormancy.dormant(npc))
            return Activity.DORMANT;
        boolean held = npc.level() instanceof ServerLevel level && NpcActivity.held(level).ids().contains(npc.getId());
        return held ? Activity.HELD : Activity.AWAKE;
    }

    private static Behaviour behaviour(NpcSafetyState safety, boolean fighting, long now)
    {
        NpcSafetyState.Action action = safety.active(now) ? safety.getAction() : NpcSafetyState.Action.NONE;
        return switch (action)
        {
            case NONE -> fighting ? Behaviour.ENGAGING : Behaviour.IDLE;
            case FIRE -> Behaviour.FIRING;
            case EVADE -> Behaviour.EVADING;
            case RETREAT -> Behaviour.RETREATING;
            case HIDE -> Behaviour.IN_COVER;
            case PEEK -> Behaviour.PEEKING;
            case FLANK -> Behaviour.FLANKING;
            case SPREAD -> Behaviour.SPREADING;
            case ADVANCE -> Behaviour.ADVANCING;
        };
    }

    private static boolean outOfAmmunition(EntityNPCInterface npc)
    {
        return npc instanceof NpcVehicleStateHolder && NpcVehicleState.of(npc).isOutOfAmmunition(npc);
    }

    private static int respawnSeconds(EntityNPCInterface npc)
    {
        if (!npc.isKilled() || npc.stats.spawnCycle < 0 || npc.stats.spawnCycle > LAST_RESPAWNING_CYCLE)
            return NEVER;
        long left = npc.killedtime - System.currentTimeMillis();
        return left <= 0L ? 0 : (int) ((left + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND);
    }
}
