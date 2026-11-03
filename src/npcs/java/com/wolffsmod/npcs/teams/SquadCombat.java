package com.wolffsmod.npcs.teams;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Short leases for movement, covering fire and cover occupancy. Pure decisions over squad-local snapshots. */
final class SquadCombat
{
    static final long RESERVATION_TICKS = 100L;
    private static final long PLANNING_TICKS = 10L;
    private static final long MOVEMENT_TICKS = 80L;

    record Member(UUID id, Vec3 position, int target, boolean ready, boolean moving, boolean reloading, boolean pinned, BotSettings.ClassRole role, boolean wantsAdvance, @Nullable Vec3 targetPosition)
    {
        Member(UUID id, Vec3 position, int target, boolean ready, boolean moving, boolean reloading, boolean pinned, BotSettings.ClassRole role, boolean wantsAdvance)
        {
            this(id, position, target, ready, moving, reloading, pinned, role, wantsAdvance, null);
        }

        @SuppressWarnings("SameParameterValue") // Retain the snapshot constructor for callers without role preferences.
        Member(UUID id, Vec3 position, int target, boolean ready, boolean moving, boolean reloading, boolean pinned)
        {
            this(id, position, target, ready, moving, reloading, pinned, BotSettings.ClassRole.RIFLEMAN, true);
        }
    }

    private record Reservation(BlockPos point, int target, long until)
    {}

    private final SquadFireTeams fireTeams = new SquadFireTeams();
    private final Map<UUID, Reservation> reservations = new HashMap<>();
    private final Map<UUID, Long> stalledMovers = new HashMap<>();
    @Nullable
    private UUID mover;
    @Nullable
    private UUID supporter;
    private long movingUntil;
    private boolean startedMoving;

    /** An empty/unsupported squad never waits for imaginary covering fire. Necessary reloads are never delayed. */
    boolean covers(Member requester, List<Member> members, long now)
    {
        Boolean coordinated = fireTeams.covers(requester, members, now);
        return coordinated != null ? coordinated : coversPair(requester, members, now);
    }

    private boolean coversPair(Member requester, List<Member> members, long now)
    {
        refreshMovement(requester, members, now);
        stalledMovers.entrySet().removeIf(entry -> entry.getValue() <= now || member(members, entry.getKey()) == null);
        if (members.stream().anyMatch(ally -> sameFight(requester, ally) && ally.moving()))
            return requester.ready();
        if (assignedMove(requester, members))
            return !requester.id().equals(mover);
        if (requester.ready() && members.stream().anyMatch(ally -> nearby(requester, ally) && ally.reloading()))
            return true;
        // Prefer an assault soldier as the next mover even when the MG happens to ask first. A soldier already at
        // its order, pinned, or waiting between bounds cannot reserve a move and leave the whole squad waiting.
        Member lead = members.stream().filter(ally -> sameFight(requester, ally) && ally.ready() && ally.wantsAdvance() && !ally.pinned())
            .filter(ally -> !stalledMovers.containsKey(ally.id()) && priority(ally.role()) > priority(requester.role())).max(Comparator.comparingInt(ally -> priority(ally.role()))).orElse(null);
        if (requester.ready() && lead != null)
        {
            reserve(lead, requester, now);
            return true;
        }
        Member covering = members.stream().filter(ally -> sameFight(requester, ally) && ally.ready() && !ally.moving()).min(Comparator.comparingInt(ally -> priority(ally.role()))).orElse(null);
        if (covering != null && !requester.pinned())
        {
            reserve(requester, covering, now);
        }
        return false;
    }

    private boolean assignedMove(Member requester, List<Member> members)
    {
        return mover != null && (mover.equals(requester.id()) || sameFight(requester, member(members, mover)));
    }

    private void reserve(Member moving, Member covering, long now)
    {
        mover = moving.id();
        supporter = covering.id();
        movingUntil = now + PLANNING_TICKS;
    }

    private static int priority(BotSettings.ClassRole role)
    {
        return switch (role)
        {
            case ASSAULT -> 2;
            case SUPPORT -> 0;
            default -> 1;
        };
    }

    private void refreshMovement(Member requester, List<Member> members, long now)
    {
        Member moving = requester.id().equals(mover) ? requester : member(members, mover);
        Member support = requester.id().equals(supporter) ? requester : member(members, supporter);
        if (movingUntil <= now && moving != null && !startedMoving && !moving.moving())
            stalledMovers.put(moving.id(), now + MOVEMENT_TICKS);
        if (movingUntil <= now || moving == null || support == null || !support.ready() || !sameFight(moving, support) || startedMoving && !moving.moving())
            clearMovement();
        else if (moving.moving() && !startedMoving)
        {
            startedMoving = true;
            movingUntil = now + MOVEMENT_TICKS;
        }
    }

    boolean available(UUID actor, BlockPos point, List<Member> members, long now)
    {
        prune(members, now);
        if (members.stream().anyMatch(member -> !member.id().equals(actor) && member.position().distanceToSqr(Vec3.atBottomCenterOf(point)) < 1D))
            return false;
        return reservations.entrySet().stream().noneMatch(entry -> !entry.getKey().equals(actor) && entry.getValue().point().distSqr(point) < 4D);
    }

    boolean claim(Member actor, BlockPos point, List<Member> members, long now)
    {
        return claim(actor.id(), actor.target(), point, members, now);
    }

    boolean claim(UUID actor, int enemy, BlockPos point, List<Member> members, long now)
    {
        if (!available(actor, point, members, now))
            return false;
        reservations.put(actor, new Reservation(point.immutable(), enemy, now + RESERVATION_TICKS));
        return true;
    }

    void release(UUID actor)
    {
        reservations.remove(actor);
        if (actor.equals(mover) || actor.equals(supporter))
            clearMovement();
    }

    private void prune(List<Member> members, long now)
    {
        reservations.entrySet().removeIf(entry ->
        {
            Member owner = member(members, entry.getKey());
            return entry.getValue().until() <= now || owner == null || owner.target() != entry.getValue().target();
        });
    }

    private void clearMovement()
    {
        mover = null;
        supporter = null;
        startedMoving = false;
    }

    @Nullable
    private static Member member(List<Member> members, @Nullable UUID id)
    {
        return id == null ? null : members.stream().filter(candidate -> candidate.id().equals(id)).findFirst().orElse(null);
    }

    /** Separate visible targets in the same enemy position can support the same manoeuvre. */
    static boolean sameFight(Member observer, @Nullable Member ally)
    {
        if (ally == null || !nearby(observer, ally) || observer.target() < 0 || ally.target() < 0)
            return false;
        return observer.target() == ally.target() || observer.targetPosition() != null && ally.targetPosition() != null && observer.targetPosition().distanceToSqr(ally.targetPosition()) <= 576D;
    }

    private static boolean nearby(Member observer, Member ally)
    {
        return !observer.id().equals(ally.id()) && observer.position().distanceToSqr(ally.position()) <= 256D;
    }
}
