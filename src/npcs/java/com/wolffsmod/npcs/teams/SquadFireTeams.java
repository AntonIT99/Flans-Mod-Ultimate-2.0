package com.wolffsmod.npcs.teams;

import org.jetbrains.annotations.Nullable;

import java.util.*;

/** Alternating elements for larger squads. Decisions are shared for five ticks, not recomputed for every soldier. */
final class SquadFireTeams
{
    private final Map<UUID, Integer> elements = new HashMap<>();
    private final Map<UUID, Boolean> decisions = new HashMap<>();
    private long sampledAt = Long.MIN_VALUE / 2;
    private long until;
    private long startBy;
    private int movingElement;
    private boolean started;

    @Nullable
    Boolean covers(SquadCombat.Member requester, List<SquadCombat.Member> members, long now)
    {
        if (members.size() < 4)
            return null;
        if (now - sampledAt >= 5L || now < sampledAt)
        {
            sampledAt = now;
            plan(requester, members, now);
        }
        return decisions.get(requester.id());
    }

    private void plan(SquadCombat.Member requester, List<SquadCombat.Member> members, long now)
    {
        List<SquadCombat.Member> local = members.stream().filter(member -> member.id().equals(requester.id()) || SquadCombat.sameFight(requester, member)).toList();
        decisions.clear();
        if (local.size() < 4)
            return;
        refreshElements(local, now);
        advanceElement(local, now);
        boolean supported = canCover(local, 1 - movingElement);
        boolean advancing = local.stream().anyMatch(member -> element(member) == movingElement && mayMove(member));
        for (SquadCombat.Member member : local)
            decisions.put(member.id(), supported && advancing && element(member) != movingElement && member.ready());
    }

    private void refreshElements(List<SquadCombat.Member> local, long now)
    {
        Set<UUID> present = new HashSet<>();
        local.forEach(member -> present.add(member.id()));
        if (!elements.keySet().equals(present))
        {
            elements.clear();
            List<SquadCombat.Member> ordered = new ArrayList<>(local);
            ordered.sort(Comparator.comparingInt((SquadCombat.Member member) -> priority(member.role())).thenComparing(SquadCombat.Member::id));
            for (int i = 0; i < ordered.size(); i++)
                elements.put(ordered.get(i).id(), i < (ordered.size() + 1) / 2 ? 0 : 1);
            movingElement = 0;
            renew(now);
        }
    }

    private void advanceElement(List<SquadCombat.Member> local, long now)
    {
        boolean moving = local.stream().anyMatch(member -> element(member) == movingElement && member.moving());
        boolean wants = local.stream().anyMatch(member -> element(member) == movingElement && mayMove(member));
        boolean covering = canCover(local, 1 - movingElement);
        if (moving)
            started = true;
        if (now >= until || !covering || !wants || started && !moving || !started && now >= startBy)
        {
            if (canCover(local, movingElement))
                movingElement = 1 - movingElement;
            renew(now);
        }
    }

    private void renew(long now)
    {
        until = now + 80L;
        startBy = now + 10L;
        started = false;
    }

    private boolean canCover(List<SquadCombat.Member> members, int element)
    {
        return members.stream().anyMatch(member -> element(member) == element && member.ready() && !member.moving());
    }

    private int element(SquadCombat.Member member)
    {
        return elements.get(member.id());
    }

    private static boolean mayMove(SquadCombat.Member member)
    {
        return member.moving() || member.wantsAdvance() && !member.pinned() && !member.reloading();
    }

    private static int priority(BotSettings.ClassRole role)
    {
        return switch (role)
        {
            case ASSAULT, MELEE -> 0;
            case SUPPORT -> 2;
            default -> 1;
        };
    }
}
