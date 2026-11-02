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

    record Member(UUID id, Vec3 position, int target, boolean ready, boolean moving, boolean reloading, boolean pinned)
    {}

    private record Reservation(BlockPos point, int target, long until)
    {}

    private final Map<UUID, Reservation> reservations = new HashMap<>();
    @Nullable
    private UUID mover;
    @Nullable
    private UUID supporter;
    private int target;
    private long movingUntil;
    private boolean startedMoving;

    /** An empty/unsupported squad never waits for imaginary covering fire. Necessary reloads are never delayed. */
    boolean covers(Member requester, List<Member> members, long now)
    {
        refreshMovement(requester, members, now);
        if (members.stream().anyMatch(ally -> nearby(requester, ally) && ally.target() == requester.target() && ally.moving()))
            return requester.ready();
        if (mover != null && target == requester.target())
            return !mover.equals(requester.id());
        if (requester.ready() && members.stream().anyMatch(ally -> nearby(requester, ally) && ally.reloading()))
            return true;
        Member covering = members.stream().filter(ally -> nearby(requester, ally) && ally.target() == requester.target() && ally.ready() && !ally.moving()).findFirst().orElse(null);
        if (covering != null && !requester.pinned())
        {
            mover = requester.id();
            supporter = covering.id();
            target = requester.target();
            movingUntil = now + PLANNING_TICKS;
        }
        return false;
    }

    private void refreshMovement(Member requester, List<Member> members, long now)
    {
        Member moving = requester.id().equals(mover) ? requester : member(members, mover);
        Member support = requester.id().equals(supporter) ? requester : member(members, supporter);
        if (movingUntil <= now || moving == null || support == null || !support.ready() || support.target() != target || moving.target() != target || startedMoving && !moving.moving())
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

    private static boolean nearby(Member observer, Member ally)
    {
        return !observer.id().equals(ally.id()) && observer.position().distanceToSqr(ally.position()) <= 256D;
    }
}
