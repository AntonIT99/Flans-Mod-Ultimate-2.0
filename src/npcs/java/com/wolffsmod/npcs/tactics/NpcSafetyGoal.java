package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.*;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.tactics.NpcSafetyState.Action;
import com.wolffsmod.npcs.teams.*;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Shared infantry safety and adaptive positions, independent of Teams membership and the current firing target. */
public final class NpcSafetyGoal extends Goal
{
    private record Situation(Vec3 position, boolean visible, boolean outmatched, boolean outranged, boolean seekCover, boolean reloading, List<Vec3> sources)
    {}

    private final EntityNPCInterface npc;
    private final NpcSafetyState state;
    private List<BlockPos> alternatives = List.of();
    private int alternative;
    private long orderedAt;
    private long attemptedAt;
    private final Map<BlockPos, Long> unreachable = new HashMap<>();
    @Nullable
    private Vec3 progressAnchor;
    private long progressedAt;
    @Nullable
    private Vec3 ordered;
    private boolean arrived;

    public NpcSafetyGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        state = NpcTacticState.of(npc).getSafety();
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public boolean canUse()
    {
        plan();
        // The dependency's projectile-slot attack owns MOVE as well: let it fire after the peek position is reached.
        return state.active(npc.level().getGameTime()) && !(state.getAction() == Action.PEEK && state.getDestination() != null && npc.position().distanceToSqr(state.getDestination()) < 0.5D);
    }

    @Override
    public void start()
    {
        npc.getNavigation().stop();
        ordered = null;
        orderedAt = npc.level().getGameTime();
    }

    @Override
    public void tick()
    {
        plan();
        Vec3 point = state.getDestination();
        if (point == null || !state.active(npc.level().getGameTime()))
            return;
        long now = npc.level().getGameTime();
        if (!keepCover(point, now))
            return;
        if (ordered == null || ordered.distanceToSqr(point) > 0.25D)
        {
            npc.getNavigation().stop();
            ordered = point;
            orderedAt = now;
            attemptedAt = now - 5L;
            progressAnchor = npc.position();
            progressedAt = now;
            arrived = false;
        }
        if (npc.position().distanceToSqr(point) < 0.5D)
        {
            arrive(point, now);
            return;
        }
        if (progressAnchor == null || npc.position().distanceToSqr(progressAnchor) > 0.25D)
        {
            progressAnchor = npc.position();
            progressedAt = now;
        }
        if (now - attemptedAt >= 5L && (npc.getNavigation().isDone() || now - attemptedAt >= 20L))
        {
            attemptedAt = now;
            npc.getNavigation().moveTo(point.x, point.y, point.z, 1D);
        }
        // A queued search is not a failure. A position that remains unreachable is replaced after a bounded wait.
        if (now - orderedAt > 60L && (npc.getNavigation().isDone() || now - progressedAt > 60L))
            retry(point, now);
    }

    private void arrive(Vec3 point, long now)
    {
        npc.getNavigation().stop();
        if (!arrived)
        {
            arrived = true;
            state.setAdvanceReadyAt(now + NpcCombatAdvance.firePause(npc));
            if (state.getAction() == Action.ADVANCE)
                order(Action.FIRE, point, NpcCombatAdvance.firePause(npc));
        }
    }

    private boolean keepCover(Vec3 point, long now)
    {
        if (state.getAction() != Action.HIDE && state.getAction() != Action.PEEK && state.getAction() != Action.ADVANCE)
            return true;
        BlockPos cover = state.getCover() == null ? BlockPos.containing(point) : state.getCover().stand();
        if (TeamsBots.claimCover(npc, cover, now))
            return true;
        retry(point, now);
        return false;
    }

    private void retry(Vec3 point, long now)
    {
        unreachable.put(BlockPos.containing(point), now + 200L);
        state.setCover(null);
        if (state.getAction() == Action.HIDE)
            while (++alternative < alternatives.size())
                if (TeamsBots.claimCover(npc, alternatives.get(alternative), now))
                {
                    order(Action.HIDE, Vec3.atBottomCenterOf(alternatives.get(alternative)), 100L);
                    return;
                }
        TeamsBots.releaseCover(npc);
        state.clear();
        state.setNextSearch(now + 20L);
        state.setAdvanceReadyAt(now + NpcCombatAdvance.firePause(npc));
    }

    @Override
    public void stop()
    {
        TeamsBots.releaseCover(npc);
        npc.getNavigation().stop();
        ordered = null;
    }

    private void plan()
    {
        long now = npc.level().getGameTime();
        if (npc.level().isClientSide || npc.isPassenger() || TacticKind.of(npc) != TacticKind.STANDARD || !NpcCombatMovement.allowed(npc))
        {
            state.clear();
            state.setAdvanceIntent(null);
            return;
        }
        if (evade(now))
            return;
        if (now < state.getNextSearch())
            return;
        state.setNextSearch(now + 10L);
        if (state.getAnchor() == null)
            state.setAnchor(npc.position());
        LivingEntity target = npc.getTarget();
        if (retreatToRefuge() || keepClearOfBlast(target))
            return;
        if (TeamsBots.movesUnderFire(npc))
        {
            // On its way to an objective: the objective goal walks it on while it fires; no stopping for cover or bounds.
            state.clear();
            state.setAdvanceIntent(null);
            return;
        }
        Situation fight = situation(target, now);
        if (fight == null)
        {
            state.clear();
            state.setAdvanceIntent(null);
            state.setAnchor(null);
            return;
        }
        updateIntent(fight, now);
        if (advance(target, fight, now))
            return;
        if (fight.seekCover() && takeCover(target, fight, now) || state.active(now))
            return;
        if (holdOrder(target))
            return;
        state.setCover(null);
        if (!flank(target, fight, now) && !spread(target))
            state.clear();
    }

    private boolean evade(long now)
    {
        Vec3 evade = evasion(npc);
        if (evade == null)
            return false;
        state.setAdvanceIntent(null);
        if (!NpcCombatMovement.permits(npc, evade))
            return false;
        order(Action.EVADE, evade, 15L);
        state.setUrgentUntil(now + 20L);
        return true;
    }

    private void updateIntent(Situation fight, long now)
    {
        Vec3 knownEnemy = fight.visible() || !NpcTacticState.of(npc).threats(now).isEmpty() ? fight.position() : null;
        NpcCombatIntent intent = !fight.outmatched() && NpcCombatAdvance.enabled(npc) ? NpcCombatAdvance.intent(npc, knownEnemy) : null;
        NpcCombatIntent previous = state.getAdvanceIntent();
        boolean changed = intent != null && (previous == null || previous.ordered() != intent.ordered() || previous.destination().distanceToSqr(intent.destination()) > 16D);
        if (intent == null || changed)
            clearAdvance();
        if (changed)
            state.setAdvanceReadyAt(now + NpcCombatAdvance.firePause(npc) + Math.floorMod(npc.getId(), 3) * 5L);
        state.setAdvanceIntent(intent);
    }

    private void clearAdvance()
    {
        if (state.getAction() == Action.ADVANCE || state.getAction() == Action.FIRE)
            state.clear();
    }

    private boolean holdOrder(@Nullable LivingEntity target)
    {
        NpcCombatIntent intent = state.getAdvanceIntent();
        if (intent == null || !intent.ordered() || !intent.reached(npc.position()) || target == null || npc.isInRange(target, Math.max(6D, npc.stats.ranged.getMeleeRange() + 2D)))
            return false;
        order(Action.FIRE, npc.position(), 15L);
        return true;
    }

    /** Move a short distance, then own MOVE while standing to aim; the equipped firing goal owns no movement flag. */
    private boolean advance(@Nullable LivingEntity target, Situation fight, long now)
    {
        NpcCombatIntent intent = state.getAdvanceIntent();
        if (intent == null || target == null || NpcDuties.retreating(npc))
            return false;
        boolean pinned = NpcSuppression.isPinned(npc);
        if (continuesMovement(fight, now, pinned))
            return true;
        if (intent.reached(npc.position()) || npc.isInRange(target, Math.max(6D, npc.stats.ranged.getMeleeRange() + 2D)))
            return false;
        if (pinned)
            return false; // The cover goal can still choose an escape, including forward cover, while pinned in the open.
        if (now < state.getAdvanceReadyAt() && !fight.reloading() || NpcCombatAdvance.coversAlly(npc, target, now))
        {
            if (fight.seekCover())
                return false; // Keep the existing hide/peek cycle until it is time to bound again.
            order(Action.FIRE, npc.position(), 15L);
            return true;
        }
        return chooseAdvance(intent, fight, now);
    }

    private boolean continuesMovement(Situation fight, long now, boolean pinned)
    {
        if (!state.active(now))
            return false;
        Action action = state.getAction();
        Vec3 destination = state.getDestination();
        if (action == Action.ADVANCE)
        {
            if (!pinned || destination != null && NpcCover.hidden(npc, fight.sources(), destination))
                return true;
            state.clear();
            return false;
        }
        if (action == Action.HIDE)
            return destination != null && npc.position().distanceToSqr(destination) >= 0.5D;
        return action != Action.FIRE && action != Action.PEEK;
    }

    private boolean chooseAdvance(NpcCombatIntent intent, Situation fight, long now)
    {
        int length = NpcCombatPolicy.value(npc, Value.ADVANCE_DISTANCE);
        unreachable.entrySet().removeIf(entry -> entry.getValue() <= now);
        NpcCover.Result<List<BlockPos>> result = !fight.sources().isEmpty() && NpcEquipment.enabled(npc, Feature.TACTICAL_COVER)
            ? NpcCover.safePositions(npc, fight.sources(), Math.min(length, NpcTuning.of(npc).get(Value.COVER_RADIUS)), intent)
            : new NpcCover.Result<>(true, List.of());
        if (!result.complete())
        {
            state.setNextSearch(now + 1L);
            return true;
        }
        List<BlockPos> cover = Objects.requireNonNull(result.value());
        for (BlockPos stand : cover)
        {
            Vec3 point = Vec3.atBottomCenterOf(stand);
            if (!unreachable.containsKey(stand) && intent.progress(npc.position(), point) >= 1D && point.subtract(npc.position()).horizontalDistanceSqr() <= (double) length * length
                && TeamsBots.claimCover(npc, stand, now))
            {
                orderAdvance(point);
                return true;
            }
        }
        // Leave room for centring the destination in its block, so a bound never grows beyond its configured length.
        Vec3 direction = intent.step(npc.position(), length - 0.75D).subtract(npc.position());
        for (float angle : new float[]{0F, 45F, -45F})
        {
            Vec3 step = npc.position().add(direction.yRot((float) Math.toRadians(angle)));
            if (advanceAt(intent, step))
                return true;
        }
        state.setAdvanceReadyAt(now + 20L);
        return false;
    }

    private boolean advanceAt(NpcCombatIntent intent, Vec3 step)
    {
        for (int height : new int[]{0, -1, 1})
        {
            BlockPos stand = BlockPos.containing(step).offset(0, height, 0);
            Vec3 point = Vec3.atBottomCenterOf(stand);
            if (!unreachable.containsKey(stand) && intent.progress(npc.position(), point) > 0.5D && NpcCombatMovement.permits(npc, point) && NpcCover.standable(npc.level(), stand)
                && TeamsBots.claimCover(npc, stand, npc.level().getGameTime()))
            {
                orderAdvance(point);
                return true;
            }
        }
        return false;
    }

    private void orderAdvance(Vec3 point)
    {
        state.setCover(null);
        order(Action.ADVANCE, point, 80L);
    }

    private boolean retreatToRefuge()
    {
        boolean personal = NpcDuties.retreating(npc);
        Optional<Vec3> squad = personal ? Optional.empty() : TeamsBots.squadWithdrawal(npc);
        if (!personal && squad.isEmpty())
            return false;
        state.setAdvanceIntent(null);
        Optional<Vec3> refuge = squad.map(point -> new NpcCombatIntent(point, 3D).step(npc.position(), 16D)).or(() -> NpcDuties.destination(npc))
            .filter(point -> NpcCombatMovement.permits(npc, point));
        if (refuge.isEmpty())
            return false;
        order(Action.RETREAT, refuge.get(), 40L);
        return true;
    }

    private boolean keepClearOfBlast(@Nullable LivingEntity target)
    {
        if (target == null || !NpcEquipment.enabled(npc, Feature.BLAST_STANDOFF) || !NpcEquipment.ranged(npc))
            return false;
        double minimum = NpcBlastSafety.standoff(npc, NpcBlastSafety.reach(NpcWeaponChoice.ammunition(npc, npc.getMainHandItem())));
        Vec3 away = npc.position().subtract(target.position()).multiply(1D, 0D, 1D).normalize();
        if (minimum > npc.distanceTo(target))
            state.setAdvanceIntent(null);
        return minimum > npc.distanceTo(target) && moveAside(Action.RETREAT, away, minimum - npc.distanceTo(target) + 3D);
    }

    @Nullable
    private Situation situation(@Nullable LivingEntity target, long now)
    {
        List<NpcTacticState.Threat> threats = NpcTacticState.of(npc).threats(now);
        LivingEntity danger = threats.stream().map(NpcTacticState.Threat::entity).filter(enemy -> enemy.level() == npc.level())
            .max(Comparator.comparingInt(enemy -> NpcVehicleThreats.groundVehicle(enemy) ? 1 : 0)).orElse(target);
        if (danger == null || !danger.isAlive())
            return null;
        boolean visible = npc.getSensing().hasLineOfSight(danger);
        boolean vehicle = NpcVehicleThreats.groundVehicle(danger);
        // A vehicle that has shot itself dry has no fight left to win, whatever is in front of it, so it breaks contact too.
        boolean spent = outOfAmmunition();
        boolean outmatched = spent
            || visible && vehicle && NpcEquipment.enabled(npc, Feature.AVOID_UNWINNABLE_FIGHTS) && NpcWeaponChoice.effectiveness(npc, danger) == NpcWeaponChoice.Effectiveness.INEFFECTIVE;
        boolean outranged = NpcEquipment.enabled(npc, Feature.OUTRANGED_COVER) && !threats.isEmpty() && !npc.isInRange(danger, NpcWeaponRange.firing(npc));
        boolean underFire = !threats.isEmpty() && NpcEquipment.enabled(npc, Feature.TACTICAL_COVER);
        List<Vec3> sources = new ArrayList<>();
        for (NpcTacticState.Threat known : threats)
            sources.add(npc.getSensing().hasLineOfSight(known.entity()) ? NpcVehicleThreats.fireOrigin(known.entity()) : known.position());
        if (visible)
            sources.add(NpcVehicleThreats.fireOrigin(danger));
        Vec3 position = visible || threats.isEmpty() ? danger.position() : threats.get(threats.size() - 1).position();
        return new Situation(position, visible, outmatched, outranged, outmatched || outranged || underFire, NpcTacticState.of(npc).isReloading(now), sources);
    }

    /** @return whether the NPC is a vehicle that has used up everything it could fire, and is allowed to give up the fight over it */
    private boolean outOfAmmunition()
    {
        return npc instanceof NpcVehicleStateHolder && NpcEquipment.enabled(npc, Feature.AVOID_UNWINNABLE_FIGHTS) && NpcVehicleState.of(npc).isOutOfAmmunition(npc);
    }

    private boolean takeCover(@Nullable LivingEntity target, Situation fight, long now)
    {
        if (cycleCover(target, fight.sources(), fight.reloading(), fight.outmatched(), now))
            return true;
        if (state.active(now) && state.getAction() != Action.PEEK && state.getAction() != Action.HIDE)
            return true;
        if (findCover(target, fight, now))
            return true;
        if (fight.outmatched() || NpcDuties.retreating(npc))
        {
            Vec3 away = npc.position().subtract(fight.position()).multiply(1D, 0D, 1D).normalize();
            return moveAside(Action.RETREAT, away, NpcCombatPolicy.value(npc, Value.WITHDRAW_DISTANCE));
        }
        // An outranged soldier bounds forward only while not outmatched; a pinned soldier can still escape sideways.
        if (fight.outranged() && !NpcSuppression.isPinned(npc))
            return moveAside(Action.FLANK, fight.position().subtract(npc.position()).multiply(1D, 0D, 1D).normalize(), 4D);
        return false;
    }

    private boolean findCover(@Nullable LivingEntity target, Situation fight, long now)
    {
        if (now < state.getCoverReadyAt())
            return false;
        unreachable.entrySet().removeIf(entry -> entry.getValue() <= now);
        NpcCombatIntent intent = fight.outmatched() ? null : state.getAdvanceIntent();
        NpcCover.Result<List<BlockPos>> result = NpcCover.safePositions(npc, fight.sources(), NpcTuning.of(npc).get(Value.COVER_RADIUS), intent);
        if (!result.complete())
        {
            state.setNextSearch(now + 1L);
            return true;
        }
        state.setCoverReadyAt(now + 40L);
        alternatives = Objects.requireNonNull(result.value()).stream().filter(point -> !unreachable.containsKey(point) && TeamsBots.coverAvailable(npc, point, now)).toList();
        if (intent == null && fight.outranged() && !fight.outmatched() && NpcCover.hidden(npc, fight.sources(), npc.position()))
            alternatives = alternatives.stream().filter(point -> Vec3.atBottomCenterOf(point).distanceTo(fight.position()) < npc.position().distanceTo(fight.position()) - 2D).toList();
        if (alternatives.isEmpty())
            return false;
        alternative = 0;
        BlockPos stand = alternatives.get(0);
        if (!TeamsBots.claimCover(npc, stand, now))
            return false;
        BlockPos peek = !fight.outmatched() && target != null && npc.isInRange(target, NpcWeaponRange.firing(npc)) ? NpcCover.peek(npc, stand, target) : null;
        state.setCover(peek == null ? null : new NpcCover.Cover(stand, peek));
        order(Action.HIDE, Vec3.atBottomCenterOf(stand), 100L);
        return true;
    }

    private boolean flank(@Nullable LivingEntity target, Situation fight, long now)
    {
        if (target == null || !npc.getSensing().hasLineOfSight(target) || !NpcVehicleThreats.groundVehicle(target) || !NpcEquipment.enabled(npc, Feature.ADAPTIVE_FLANKING)
            || now < state.getFlankReadyAt() || !NpcWeaponChoice.antiArmor(npc, npc.getMainHandItem()) || fight.reloading())
            return false;
        Vec3 offset = npc.position().subtract(NpcVehicleThreats.platform(target).position()).multiply(1D, 0D, 1D);
        if (offset.normalize().dot(NpcVehicleThreats.front(target)) <= 0.5D)
            return false;
        Vec3 side = offset.yRot((npc.getId() % 2 == 0 ? 1F : -1F) * (float) Math.toRadians(65D));
        Vec3 destination = NpcVehicleThreats.platform(target).position().add(side);
        if (!moveAside(Action.FLANK, destination.subtract(npc.position()).normalize(), Math.min(8D, npc.position().distanceTo(destination))))
            return false;
        state.setFlankReadyAt(now + 160L);
        return true;
    }

    private boolean spread(@Nullable LivingEntity target)
    {
        if (target == null || !NpcEquipment.enabled(npc, Feature.ALLY_SPACING) || NpcSuppression.isPinned(npc) || !(npc.level() instanceof ServerLevel level))
            return false;
        int gap = NpcCombatPolicy.value(npc, Value.ALLY_GAP);
        NpcCombatants combatants = NpcCombatants.of(level);
        for (int index = 0; index < combatants.npcSize(); index++)
        {
            EntityNPCInterface ally = combatants.npc(index).npc();
            if (ally == null || ally == npc || ally.distanceToSqr(npc) >= (double) gap * gap || !NpcDrivers.ally(npc, ally))
                continue;
            Vec3 away = npc.position().subtract(ally.position()).multiply(1D, 0D, 1D);
            if (away.lengthSqr() < 1.0E-6D)
                away = Vec3.directionFromRotation(0F, npc.getId() * 137.5F);
            if (moveAside(Action.SPREAD, away.normalize(), gap))
                return true;
        }
        return false;
    }

    private boolean cycleCover(@Nullable LivingEntity target, List<Vec3> sources, boolean reloading, boolean outmatched, long now)
    {
        NpcCover.Cover cover = state.getCover();
        if (cover == null || target == null || outmatched || !NpcCover.hidden(npc, sources, cover.standPoint()))
        {
            state.setCover(null);
            return state.active(now) && state.getAction() == Action.HIDE;
        }
        boolean hiding = state.getAction() == Action.HIDE;
        if (hiding && !state.active(now) && npc.position().distanceToSqr(cover.standPoint()) >= 2D)
        {
            state.setCover(null);
            return false;
        }
        if (!hiding && (reloading || !state.active(now)))
            order(Action.HIDE, cover.standPoint(), 30L);
        else if (hiding && !reloading && !state.active(now) && npc.position().distanceToSqr(cover.standPoint()) < 2D)
            order(Action.PEEK, cover.peekPoint(), 40L);
        return true;
    }

    private boolean moveAside(Action action, Vec3 direction, double distance)
    {
        for (float angle : new float[]{0F, 45F, -45F, 90F, -90F})
        {
            Vec3 point = npc.position().add(direction.yRot((float) Math.toRadians(angle)).scale(distance));
            BlockPos stand = BlockPos.containing(point);
            if (NpcCombatMovement.permits(npc, point) && NpcCover.standable(npc.level(), stand))
            {
                order(action, Vec3.atBottomCenterOf(stand), 60L);
                return true;
            }
        }
        return false;
    }

    private void order(Action action, Vec3 point, long ticks)
    {
        state.setAction(action);
        state.setDestination(point);
        state.setUntil(npc.level().getGameTime() + ticks);
    }

    /** Cheap, short-horizon collision check also used to wake distant NPCs before the goal selector runs. */
    @Nullable
    public static Vec3 evasion(EntityNPCInterface npc)
    {
        if (!NpcEquipment.enabled(npc, Feature.VEHICLE_EVASION) || npc.isPassenger() || TacticKind.of(npc) != TacticKind.STANDARD || !NpcCombatMovement.allowed(npc))
            return null;
        NpcSafetyState state = NpcTacticState.of(npc).getSafety();
        long now = npc.level().getGameTime();
        if (now - state.getCollisionCheckedAt() < 3L && now >= state.getCollisionCheckedAt())
            return state.getEvasion();
        state.setCollisionCheckedAt(now);
        state.setEvasion(null);
        if (!(npc.level() instanceof ServerLevel level))
            return null;
        NpcCombatants combatants = NpcCombatants.of(level);
        for (int index = 0; index < combatants.vehicleSize(); index++)
        {
            Entity vehicle = combatants.vehicle(index);
            Vec3 point = escapeFrom(npc, vehicle);
            if (point != null && NpcCover.standable(npc.level(), BlockPos.containing(point)))
            {
                state.setEvasion(point);
                state.setUrgentUntil(now + 20L);
                return point;
            }
        }
        return null;
    }

    @Nullable
    private static Vec3 escapeFrom(EntityNPCInterface npc, Entity vehicle)
    {
        if (vehicle == NpcVehicleThreats.platform(npc) || vehicle.hasIndirectPassenger(npc))
            return null;
        Vec3 velocity = new Vec3(vehicle.getX() - vehicle.xo, 0D, vehicle.getZ() - vehicle.zo);
        if (velocity.lengthSqr() > 4D)
            velocity = vehicle.getDeltaMovement().multiply(1D, 0D, 1D);
        return velocity.horizontalDistanceSqr() < 0.0025D ? null : escape(npc.position(), npc.getBoundingBox(), NpcVehicleThreats.hull(vehicle), velocity);
    }

    @Nullable
    static Vec3 escape(Vec3 position, AABB body, AABB vehicle, Vec3 velocity)
    {
        if (velocity.horizontalDistanceSqr() < 0.0025D || !vehicle.expandTowards(velocity.scale(12D)).inflate(0.75D).intersects(body))
            return null;
        Vec3 side = new Vec3(-velocity.z, 0D, velocity.x).normalize();
        double offset = position.subtract(vehicle.getCenter()).dot(side);
        double halfWidth = (Math.abs(side.x) * vehicle.getXsize() + Math.abs(side.z) * vehicle.getZsize()) * 0.5D;
        double step = Math.max(2D, halfWidth + body.getXsize() * 0.5D + 1D - Math.abs(offset));
        return position.add(side.scale((offset >= 0D ? 1D : -1D) * step));
    }
}
