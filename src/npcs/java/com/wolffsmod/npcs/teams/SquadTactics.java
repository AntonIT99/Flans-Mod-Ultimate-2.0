package com.wolffsmod.npcs.teams;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Pure squad leadership over commander snapshots. No entity references, world queries, or path searches. */
final class SquadTactics
{
    /** Blocks within which an enemy is close enough that a travelling soldier stops to fight it. */
    static final double CLOSE_CONTACT = 12D;
    /** Blocks from its spot beyond which a soldier still counts as travelling. */
    private static final double TRANSIT_ARRIVAL = 4D;

    enum Phase
    {
        APPROACH, SUPPORT, ASSAULT, SECURE, REGROUP, WITHDRAW
    }

    enum Report
    {
        NONE, BLOCKED, PINNED, SECURED
    }

    record Context(SquadTask task, Vec3 destination, double readiness, Vec3 refuge, @Nullable Vec3 threat, boolean secured, @Nullable Vec3 coveredRally, List<Vec3> posts)
    {}

    record Directive(Vec3 spot, double arrival, SquadTask.Kind kind, double radius, boolean retreat, Vec3 facing)
    {}

    private final Map<UUID, Directive> orders = new HashMap<>();
    @Nullable
    private SquadTask mission;
    @Nullable
    private UUID leader;
    @Getter
    private Phase phase = Phase.APPROACH;
    private long phaseSince;
    private Vec3 anchor = Vec3.ZERO;
    private Vec3 facing = new Vec3(1D, 0D, 0D);
    private long regroupAfter;
    private long withdrawAfter;
    private long pinnedSince = -1L;
    private long progressedAt;
    private double bestDistance = Double.POSITIVE_INFINITY;
    @Nullable
    private Vec3 progressDestination;
    private Report report = Report.NONE;
    private long reportUntil;
    private int failures;
    private long failedAt = Long.MIN_VALUE / 2;
    private boolean retry;
    @Nullable
    private Vec3 failedTarget;
    private long avoidUntil;

    void clear()
    {
        orders.clear();
        mission = null;
        leader = null;
        retry = false;
        report = Report.NONE;
        pinnedSince = -1L;
        failedTarget = null;
        avoidUntil = 0L;
        failures = 0;
        regroupAfter = 0L;
        withdrawAfter = 0L;
    }

    void update(Context context, List<SquadCombat.Member> members, long now)
    {
        if (members.isEmpty())
        {
            clear();
            return;
        }
        SquadCombat.Member first = members.get(0);
        begin(context, first, now);
        long pinned = members.stream().filter(SquadCombat.Member::pinned).count();
        updatePressure(context, members.size(), pinned, now);
        if (!recover(context, members, now))
            planMission(context, members, now);
        orders.clear();
        Set<Vec3> occupied = new HashSet<>();
        boolean atRefuge = majorityNear(members, anchor, 12D);
        for (int i = 0; i < members.size(); i++)
        {
            SquadCombat.Member member = members.get(i);
            orders.put(member.id(), directive(context, members.size(), member, i, occupied, atRefuge, now));
        }
    }

    private void begin(Context context, SquadCombat.Member first, long now)
    {
        if (!matches(context.task()))
        {
            mission = context.task();
            failures = 0;
            report = Report.NONE;
            retry = false;
            facing = direction(first.position(), context.task().target());
            transition(Phase.APPROACH, first.position(), now);
        }
        if (!first.id().equals(leader))
        {
            leader = first.id();
            progressedAt = now;
            bestDistance = Double.POSITIVE_INFINITY;
        }
    }

    private void updatePressure(Context context, int size, long pinned, long now)
    {
        if (pinned * 2 >= size)
        {
            if (pinnedSince < 0L)
                pinnedSince = now;
            if (now - pinnedSince >= 60L)
                report(Report.PINNED, now);
        }
        else
            pinnedSince = -1L;
        boolean broken = context.readiness() < 0.35D || pinnedSince >= 0L && now - pinnedSince >= 80L && context.readiness() < 0.55D;
        if (phase != Phase.WITHDRAW && now >= withdrawAfter && broken && (context.threat() != null || pinned > 0L))
        {
            transition(Phase.WITHDRAW, context.refuge(), now);
            report(Report.PINNED, now);
        }
    }

    /** Recovery has strict deadlines and cooldowns; a stranded member cannot repeatedly stop the squad. */
    private boolean recover(Context context, List<SquadCombat.Member> members, long now)
    {
        if (phase == Phase.WITHDRAW)
        {
            if (now - phaseSince >= 200L || now - phaseSince >= 40L && majorityNear(members, anchor, 12D))
            {
                withdrawAfter = now + 600L;
                transition(Phase.REGROUP, anchor, now);
            }
            return true;
        }
        if (phase == Phase.REGROUP)
        {
            if (now - phaseSince < 200L && (now - phaseSince < 60L || !majorityNear(members, anchor, 16D)))
                return true;
            regroupAfter = now + 400L;
            transition(Phase.APPROACH, members.get(0).position(), now);
        }
        Vec3 position = members.get(0).position();
        if (now >= regroupAfter && distance(position, context.destination()) > 20D && !majorityNear(members, position, 24D))
        {
            Vec3 rally = context.coveredRally() == null ? position : context.coveredRally();
            transition(Phase.REGROUP, rally, now);
            return true;
        }
        return false;
    }

    private void planMission(Context context, List<SquadCombat.Member> members, long now)
    {
        if (secure(context, members, now))
            return;
        // A report elsewhere on the map must not stop a squad before it has travelled around the fighting.
        boolean bypassing = distance(context.destination(), context.task().target()) > 8D;
        boolean contact = context.threat() != null && distance(members.get(0).position(), context.threat()) <= 24D;
        if (phase == Phase.APPROACH && !bypassing && (contact || distance(members.get(0).position(), context.destination()) <= 12D))
            transition(Phase.SUPPORT, context.coveredRally() == null ? members.get(0).position() : context.coveredRally(), now);
        if (phase == Phase.SUPPORT && now - phaseSince >= 20L && (supportReady(members) || now - phaseSince >= 80L))
            transition(Phase.ASSAULT, anchor, now);
        if (phase == Phase.APPROACH || phase == Phase.ASSAULT)
            progress(context.destination(), members.get(0), now);
    }

    private boolean secure(Context context, List<SquadCombat.Member> members, long now)
    {
        SquadTask task = context.task();
        boolean defensive = task.kind() == SquadTask.Kind.DEFEND || task.kind() == SquadTask.Kind.GUARD;
        if (!defensive && !(context.secured() && majorityNear(members, task.target(), task.radius() + 8D)))
            return false;
        if (phase != Phase.SECURE)
        {
            transition(Phase.SECURE, task.target(), now);
            if (context.threat() != null)
                facing = direction(task.target(), context.threat());
        }
        if (context.secured() && report(now) != Report.PINNED)
            report(Report.SECURED, now);
        return true;
    }

    private static boolean supportReady(List<SquadCombat.Member> members)
    {
        return members.stream().anyMatch(member -> member.ready() && !member.moving() && member.role() == BotSettings.ClassRole.SUPPORT)
            || members.stream().skip((members.size() + 1L) / 2L).anyMatch(member -> member.ready() && !member.moving());
    }

    private void progress(Vec3 destination, SquadCombat.Member first, long now)
    {
        if (progressDestination == null || distance(progressDestination, destination) > 8D)
        {
            progressDestination = destination;
            bestDistance = Double.POSITIVE_INFINITY;
            progressedAt = now;
        }
        double remaining = distance(first.position(), destination);
        if (remaining < 12D || remaining <= bestDistance - 2D)
        {
            bestDistance = remaining;
            progressedAt = now;
        }
        else if (!first.pinned() && now - progressedAt >= 200L)
            blocked(first.id(), now);
    }

    void blocked(UUID actor, long now)
    {
        if (!actor.equals(leader) || mission == null || phase != Phase.APPROACH && phase != Phase.ASSAULT || now - failedAt < 200L)
            return;
        failedAt = now;
        progressedAt = now;
        bestDistance = Double.POSITIVE_INFINITY;
        failures++;
        retry = true;
        report(Report.BLOCKED, now);
        if (failures >= 2)
        {
            failedTarget = mission.target();
            avoidUntil = now + 600L;
        }
    }

    private Directive directive(Context context, int size, SquadCombat.Member member, int index, Set<Vec3> occupied, boolean atRefuge, long now)
    {
        if (phase == Phase.SECURE)
        {
            Vec3 spot = sector(context, member, index, size, occupied);
            return new Directive(spot, 1.5D, SquadTask.Kind.DEFEND, 4D, false, facing);
        }
        if (phase == Phase.WITHDRAW)
        {
            // A loaded rear guard covers the first withdrawal, then follows even if somebody never reaches safety.
            boolean guard = member.ready() && index >= (size + 1) / 2 && now - phaseSince < 60L && !atRefuge;
            Vec3 spot = guard ? member.position() : BotFormation.spot(anchor, index, 6D, List.of());
            return new Directive(spot, 3D, SquadTask.Kind.HOLD, 6D, !guard, facing);
        }
        if (phase == Phase.REGROUP || phase == Phase.SUPPORT)
            return new Directive(BotFormation.spot(anchor, index, 6D, List.of()), 3D, SquadTask.Kind.HOLD, 8D, false, facing);
        double radius = context.task().radius();
        boolean capture = context.task().kind() == SquadTask.Kind.ATTACK && distance(context.destination(), context.task().target()) <= 8D;
        Vec3 spot = BotFormation.spot(context.destination(), index, capture ? Math.min(3D, radius * 0.5D) : radius, List.of());
        return new Directive(spot, capture ? Math.min(1D, radius * 0.25D) : 2.5D, context.task().kind(), radius, false, facing);
    }

    private Vec3 sector(Context context, SquadCombat.Member member, int index, int size, Set<Vec3> occupied)
    {
        double angle = Math.atan2(facing.z, facing.x) + 2D * Math.PI * index / Math.max(1, size);
        double radius = Math.max(1D, Math.min(10D, context.task().radius() * 0.8D));
        boolean close = member.role() == BotSettings.ClassRole.ASSAULT || member.role() == BotSettings.ClassRole.MELEE;
        if (member.role() == BotSettings.ClassRole.SUPPORT)
            angle = Math.atan2(facing.z, facing.x);
        else if (member.role() == BotSettings.ClassRole.MEDIC)
            angle = Math.atan2(facing.z, facing.x) + Math.PI;
        else if (close)
            radius *= 0.35D;
        Vec3 desired = context.task().target().add(Math.cos(angle) * radius, 0D, Math.sin(angle) * radius);
        Vec3 spot = close ? desired : context.posts().stream().filter(post -> !occupied.contains(post)).min(Comparator.comparingDouble(post -> post.distanceToSqr(desired))).orElse(desired);
        // Two support weapons still need separate stands when no authored posts exist.
        if (occupied.contains(spot))
            spot = BotFormation.spot(desired, index + 1, 3D, List.of());
        occupied.add(spot);
        return spot;
    }

    /**
     * Whether a soldier on its way to an objective keeps moving while it fires, rather than stopping to fight, or chasing,
     * whatever enemy it sees. In a large battle somebody is always in sight, so without this no squad would ever leave the
     * front line for an objective. An enemy close by is still fought, and a pinned soldier only keeps going where its way
     * leads no closer to the enemy.
     */
    static boolean transit(Phase phase, SquadTask.Kind kind, Vec3 position, Vec3 spot, double arrival, Vec3 enemy, boolean pinned)
    {
        if (phase != Phase.APPROACH && phase != Phase.ASSAULT || kind != SquadTask.Kind.ATTACK && kind != SquadTask.Kind.DEFEND && kind != SquadTask.Kind.GUARD)
            return false;
        if (distance(position, spot) <= Math.max(arrival, TRANSIT_ARRIVAL) || distance(position, enemy) <= CLOSE_CONTACT)
            return false;
        return !pinned || distance(spot, enemy) >= distance(position, enemy);
    }

    @Nullable
    Directive order(SquadTask task, UUID actor)
    {
        return matches(task) ? orders.get(actor) : null;
    }

    @Nullable
    Vec3 failedTarget(long now)
    {
        return now < avoidUntil ? failedTarget : null;
    }

    @Nullable
    Vec3 supportTarget(long now)
    {
        return mission != null && report(now) == Report.PINNED ? mission.target() : null;
    }

    Report report(long now)
    {
        return now < reportUntil ? report : Report.NONE;
    }

    boolean takeRetry()
    {
        boolean requested = retry;
        retry = false;
        return requested;
    }

    private void report(Report next, long now)
    {
        report = next;
        reportUntil = now + 120L;
    }

    private boolean matches(SquadTask task)
    {
        return mission != null && mission.kind() == task.kind() && mission.target().distanceToSqr(task.target()) <= 256D;
    }

    private void transition(Phase next, Vec3 at, long now)
    {
        phase = next;
        phaseSince = now;
        anchor = at;
        progressedAt = now;
        bestDistance = Double.POSITIVE_INFINITY;
    }

    private static boolean majorityNear(List<SquadCombat.Member> members, Vec3 point, double radius)
    {
        return members.stream().filter(member -> distance(member.position(), point) <= radius).count() * 3 >= members.size() * 2L;
    }

    private static Vec3 direction(Vec3 from, Vec3 to)
    {
        Vec3 direction = to.subtract(from).multiply(1D, 0D, 1D);
        return direction.lengthSqr() < 1.0E-6D ? new Vec3(1D, 0D, 0D) : direction.normalize();
    }

    private static double distance(Vec3 from, Vec3 to)
    {
        return from.subtract(to).horizontalDistance();
    }
}
