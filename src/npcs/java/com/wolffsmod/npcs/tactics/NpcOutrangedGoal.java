package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcSight;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.TacticKind;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Keeps an NPC on foot out of the sight of an enemy that shoots at it from beyond its reach, instead of walking up to the
 * enemy in the open: it takes the nearest cover from the enemy, waits there a moment, then moves up on it from cover to
 * cover, or a few blocks at a time where it finds none, until the enemy is within its reach and its tactic takes over
 * again. One that holds its position only takes cover. It comes before every other goal that moves an NPC in a fight.
 * Server side.
 */
public final class NpcOutrangedGoal extends Goal
{
    /** Ticks between two searches for cover. */
    static final int SEARCH_INTERVAL = 20;
    /** Fewest and most ticks the NPC waits in cover before it moves up. */
    static final int HOLD_MIN = 40;
    static final int HOLD_MAX = 100;
    /** Blocks nearer the enemy the next cover must be for the NPC to move up to it. */
    static final double MIN_ADVANCE = 4D;
    /** Path nodes the NPC dashes over where it finds no cover ahead. */
    static final int DASH_NODES = 8;
    /** Ticks after which the NPC gives up going where it was sent. */
    static final int MOVE_TIMEOUT = 100;
    private static final double SPEED = 1D;

    private final EntityNPCInterface npc;
    /** Where the NPC is going, or null while it waits. */
    @Nullable
    private BlockPos destination;
    private long movedAt;
    private long holdUntil;
    private long nextSearch;

    public NpcOutrangedGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    /** @return whether the NPC's target is an enemy shooting at it from beyond its reach, from which it takes cover */
    public static boolean outranged(EntityNPCInterface npc)
    {
        if (!NpcEquipment.enabled(npc, Feature.OUTRANGED_COVER) || npc.isPassenger() || !NpcEquipmentRangedGoal.engages(npc) || TacticKind.of(npc) != TacticKind.STANDARD)
            return false;
        LivingEntity target = npc.getTarget();
        return target != null && NpcThreats.threat(npc) == target && !npc.isInRange(target, NpcWeaponRange.firing(npc));
    }

    @Override
    public boolean canUse()
    {
        return outranged(npc);
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void start()
    {
        destination = null;
        holdUntil = 0L;
        nextSearch = 0L;
    }

    @Override
    public void tick()
    {
        LivingEntity enemy = npc.getTarget();
        if (enemy == null)
            return;
        long now = npc.level().getGameTime();
        npc.getLookControl().setLookAt(enemy, 30F, 30F);
        if (destination != null)
        {
            if (!npc.getNavigation().isDone() && now - movedAt < MOVE_TIMEOUT)
                return;
            destination = null;
            npc.getNavigation().stop();
            holdUntil = now + HOLD_MIN + npc.getRandom().nextInt(HOLD_MAX - HOLD_MIN + 1);
        }
        // Whether the enemy sees it, and where to go next, are looked at once an interval: each costs a ray trace or more.
        if (now < nextSearch)
            return;
        nextSearch = now + SEARCH_INTERVAL;
        boolean hidden = !NpcSight.clear(npc.level(), enemy.getEyePosition(), npc.getEyePosition(), 0, npc);
        if (hidden && now < holdUntil)
            return;
        int radius = NpcTuning.of(npc).get(Value.COVER_RADIUS);
        if (!hidden)
        {
            BlockPos cover = NpcCover.hide(npc, enemy, npc.blockPosition(), radius, Double.MAX_VALUE);
            if (cover != null)
            {
                go(cover, now);
                return;
            }
        }
        if (NpcTactics.of(npc) != Tactic.HOLD)
            advance(enemy, radius, now);
    }

    @Override
    public void stop()
    {
        destination = null;
        npc.getNavigation().stop();
    }

    /** Moves up on the enemy: to the nearest cover a cover search ahead finds nearer it, or else a few blocks along the path to it. */
    private void advance(LivingEntity enemy, int radius, long now)
    {
        Vec3 toward = enemy.position().subtract(npc.position()).multiply(1D, 0D, 1D);
        double distance = toward.length();
        if (distance < 1D)
            return;
        Vec3 ahead = npc.position().add(toward.scale(Math.min(distance, radius) / distance));
        BlockPos cover = NpcCover.hide(npc, enemy, BlockPos.containing(ahead), radius, npc.position().distanceTo(enemy.position()) - MIN_ADVANCE);
        if (cover != null)
        {
            go(cover, now);
            return;
        }
        Path path = npc.getNavigation().createPath(enemy, 1);
        if (path == null || path.getNodeCount() == 0)
            return;
        path.truncateNodes(Math.min(path.getNodeCount(), DASH_NODES));
        if (npc.getNavigation().moveTo(path, SPEED))
        {
            destination = path.getEndNode() == null ? npc.blockPosition() : path.getEndNode().asBlockPos();
            movedAt = now;
        }
    }

    private void go(BlockPos spot, long now)
    {
        if (!npc.getNavigation().moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, SPEED))
            return;
        destination = spot;
        movedAt = now;
    }
}
