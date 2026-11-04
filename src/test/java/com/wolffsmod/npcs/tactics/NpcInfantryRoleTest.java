package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.teams.BotSettings.ClassRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcInfantryRoleTest
{
    @Test
    void assaultClosesButRespectsExplosiveSafetyAndExplicitDistances()
    {
        Engagement rifle = new Engagement(30D, 60D, 0D);
        Engagement assault = NpcInfantryRole.engagement(rifle, ClassRole.ASSAULT, true);
        assertEquals(16D, assault.preferred());
        assertEquals(rifle.firing(), assault.firing());
        assertSame(rifle, NpcInfantryRole.engagement(rifle, ClassRole.ASSAULT, false));
        assertSame(rifle, NpcInfantryRole.engagement(rifle, ClassRole.RIFLEMAN, true));
        Engagement explosive = new Engagement(30D, 60D, 24D);
        assertEquals(24D, NpcInfantryRole.engagement(explosive, ClassRole.ASSAULT, true).preferred());
    }

    @Test
    void skirmishersKeepMoreDistanceWithoutExceedingTheirWeaponRange()
    {
        Engagement bow = new Engagement(18D, 40D, 0D);
        assertEquals(32D, NpcInfantryRole.engagement(bow, ClassRole.SKIRMISHER, true).preferred());
        Engagement shortThrow = new Engagement(2D, 6D, 0D);
        assertEquals(6D, NpcInfantryRole.engagement(shortThrow, ClassRole.SKIRMISHER, true).preferred());
        assertSame(bow, NpcInfantryRole.engagement(bow, ClassRole.SKIRMISHER, false));
    }

    @Test
    void assaultMovesSoonerAndMachineGunsSpendLongerCovering()
    {
        assertEquals(20, NpcInfantryRole.firePause(40, ClassRole.ASSAULT, false));
        assertEquals(40, NpcInfantryRole.firePause(40, ClassRole.RIFLEMAN, false));
        assertEquals(80, NpcInfantryRole.firePause(40, ClassRole.SUPPORT, false));
        assertEquals(80, NpcInfantryRole.firePause(40, ClassRole.RIFLEMAN, true));
        assertEquals(10, NpcInfantryRole.firePause(10, ClassRole.ASSAULT, false));
    }

    @Test
    void existingSavedDutyIndicesRemainUnchanged()
    {
        assertEquals(0, NpcTuning.Duty.NONE.ordinal());
        assertEquals(5, NpcTuning.Duty.ANTI_TANK.ordinal());
        assertEquals(6, NpcTuning.Duty.ASSAULT.ordinal());
        assertEquals(7, NpcTuning.Duty.MELEE.ordinal());
        assertEquals(8, NpcTuning.Duty.SKIRMISHER.ordinal());
    }
}
