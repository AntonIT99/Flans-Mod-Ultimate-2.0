package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcVehicleCombat;
import com.wolffsmod.npcs.sim.NpcFiringPosition;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.tactics.NpcCover.Cover;
import com.wolffsmod.npcs.tactics.TacticDecision.Order;
import com.wolffsmod.npcs.tactics.TacticDecision.Situation;
import com.wolffsmod.npcs.teams.NpcSuppression;
import com.wolffsmod.npcs.teams.TeamsBots;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Carries out a tactic for one tick: looks for cover when the tactic needs it, decides, and hands the path to the NPC's
 * navigator. Whichever goal moves the NPC calls it, the one for an equipped weapon and Custom NPCs' own ranged attack
 * alike. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcTacticSteering
{
    /** Ticks between searches for cover while the NPC has none, and between checks that the cover it has still hides it. */
    static final int COVER_SEARCH_INTERVAL = 40;
    static final int COVER_CHECK_INTERVAL = 20;
    /** Ticks after a hit during which an NPC fighting from cover counts as hit. */
    static final int HIT_TICKS = 20;
    /** Blocks the point of an order may move before the NPC is sent there again. */
    private static final double REPATH_DISTANCE_SQR = 1D;
    /** Ticks before the NPC is sent again to a point it has stopped short of. */
    static final int REPATH_TICKS = 10;
    /** Custom NPCs' flying movement type. */
    private static final int FLYING = 1;

    public static void steer(EntityNPCInterface npc, Tactic tactic)
    {
        LivingEntity target = npc.getTarget();
        if (target == null)
            return;
        if (NpcInfantryAirDefense.holdsPosition(npc, target))
        {
            npc.getNavigation().stop();
            return;
        }
        long now = npc.level().getGameTime();
        NpcTacticState memory = NpcTacticState.of(npc);
        memory.prepare(tactic, target.getId(), now, (npc.getId() & 1) == 0 ? 1 : -1);
        // A transport keeps its tactic distance rather than firing from it, so its own distances stand in for a weapon's.
        Engagement engagement = tactic == Tactic.TRANSPORT ? NpcTactics.unloading(npc) : NpcTactics.engagement(npc);
        if (tactic == Tactic.COVER)
            refreshCover(npc, target, memory, engagement, now);
        boolean hit = npc.getLastHurtByMob() != null && npc.tickCount - npc.getLastHurtByMobTimestamp() < HIT_TICKS;
        Vec3 home = new Vec3(npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
        if (TeamsBots.isCommander(npc))
            home = TeamsBots.combatIntent(npc).map(NpcCombatIntent::destination).orElse(home);
        Situation situation = new Situation(npc.getSensing().hasLineOfSight(target), npc.position(), target.position(), home, engagement, memory.isReloading(now), hit);
        Order order = TacticDecision.decide(tactic, situation, memory, now);
        // A vehicle without a shot from where it would stand, or close in from, drives to where it has one; one that holds or
        // carries troops stays put, a transport having no shot to look for.
        if (tactic != Tactic.HOLD && tactic != Tactic.TRANSPORT && (order.move() != TacticDecision.Move.GO_TO || NpcVehicleCombat.armorFlank(npc, target) != null)
            && NpcFiringPosition.reposition(npc, target))
        {
            memory.order(null, now);
            return;
        }
        follow(npc, order, memory, target, now);
    }

    private static void refreshCover(EntityNPCInterface npc, LivingEntity target, NpcTacticState memory, Engagement engagement, long now)
    {
        Cover cover = memory.getCover();
        if (cover != null)
        {
            if (now - memory.getCoverSearchedAt() < COVER_CHECK_INTERVAL)
                return;
            if (!NpcCover.exposed(npc, target, cover) && TeamsBots.claimCover(npc, cover.stand(), now))
            {
                // Still hidden: checked again an interval from now.
                memory.cover(cover, now);
                return;
            }
        }
        else if (now - memory.getCoverSearchedAt() < COVER_SEARCH_INTERVAL)
            return;
        NpcCover.Result<Cover> result = NpcCover.find(npc, target, NpcTuning.of(npc).get(Value.COVER_RADIUS), engagement);
        if (result.complete())
        {
            Cover found = result.value();
            memory.cover(found != null && TeamsBots.claimCover(npc, found.stand(), now) ? found : null, now);
        }
    }

    private static void follow(EntityNPCInterface npc, Order order, NpcTacticState memory, LivingEntity target, long now)
    {
        PathNavigation navigation = npc.getNavigation();
        if (NpcSuppression.isPinned(npc) && advances(npc, order, target))
        {
            // Pinned down by fire, it moves no closer to its target until the fire lets up; it may still fall back.
            navigation.stop();
            memory.order(null, now);
            return;
        }
        switch (order.move())
        {
            case STAND -> {
                navigation.stop();
                memory.order(null, now);
            }
            case CLOSE_IN -> {
                navigation.moveTo(target, order.speed());
                memory.order(null, now);
            }
            case GO_TO -> {
                Vec3 wanted = Objects.requireNonNull(order.point());
                // A flying NPC keeps its height, which its flight holds; it only moves across.
                Vec3 point = npc.ais.movementType == FLYING ? new Vec3(wanted.x, npc.getY(), wanted.z) : wanted;
                Vec3 ordered = memory.getOrdered();
                // A point it could not find a way to is tried again only after a while, not on every tick.
                boolean retry = navigation.isDone() && now - memory.getOrderedAt() >= REPATH_TICKS;
                if (ordered == null || ordered.distanceToSqr(point) > REPATH_DISTANCE_SQR || retry)
                {
                    navigation.moveTo(point.x, point.y, point.z, order.speed());
                    memory.order(point, now);
                }
            }
        }
    }

    /** Whether the order takes the NPC closer to its target. */
    private static boolean advances(EntityNPCInterface npc, Order order, LivingEntity target)
    {
        return switch (order.move())
        {
            case STAND -> false;
            case CLOSE_IN -> true;
            case GO_TO -> Objects.requireNonNull(order.point()).distanceToSqr(target.position()) < npc.distanceToSqr(target);
        };
    }
}
