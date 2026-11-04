package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.teams.BotSettings.ClassRole;
import com.wolffsmod.npcs.teams.NpcDuties;
import com.wolffsmod.npcs.teams.TeamsBots;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

/** Infantry fighting preferences shared by battle classes and ordinary soldier duties. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcInfantryRole
{
    public static ClassRole of(EntityNPCInterface npc)
    {
        return TeamsBots.classRole(npc).orElseGet(() -> NpcDuties.role(npc));
    }

    public static boolean support(EntityNPCInterface npc)
    {
        return of(npc) == ClassRole.SUPPORT || NpcTactics.role(npc) == WeaponRole.SUPPORT;
    }

    /** Explicit distance settings and explosive stand-off remain authoritative. */
    static Engagement engagement(Engagement base, ClassRole role, boolean automatic)
    {
        if (!automatic)
            return base;
        if (role == ClassRole.SKIRMISHER)
            return new Engagement(Math.max(base.minimum(), Math.min(base.firing(), Math.max(8D, Math.min(32D, base.firing() * 0.8D)))), base.firing(), base.minimum());
        if (role != ClassRole.ASSAULT)
            return base;
        double preferred = Math.max(base.minimum(), Math.max(4D, Math.min(16D, base.preferred() * 0.6D)));
        return new Engagement(Math.min(preferred, base.firing()), base.firing(), base.minimum());
    }

    static int firePause(int configured, ClassRole role, boolean machineGun)
    {
        if (role == ClassRole.ASSAULT)
            return Math.max(10, configured / 2);
        return role == ClassRole.SUPPORT || machineGun ? Math.min(160, configured * 2) : configured;
    }
}
