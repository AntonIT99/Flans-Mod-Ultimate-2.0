package com.flansmodultimate.common;

import com.flansmodultimate.common.explosions.ExplosionKillAudit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExplosionKillAuditTest
{
    @Test
    void spawnKillWarningUsesExclusiveLegacyThreshold()
    {
        assertTrue(ExplosionKillAudit.isPossibleSpawnKill(9, 10));
        assertFalse(ExplosionKillAudit.isPossibleSpawnKill(10, 10));
        assertFalse(ExplosionKillAudit.isPossibleSpawnKill(0, 0));
    }

    @Test
    void detailedRecordContainsAllLegacyAuditFields()
    {
        assertEquals(
            "Explosion kill: killer=Alice victim=Bob weapon=M67 victimLifetimeSeconds=4 "
                + "victimPos=(1,2,3) killerPos=(4,5,6) "
                + "victimChestArmor=item.flansmod.body_armour killerChestArmor=none",
            ExplosionKillAudit.formatKillRecord(
                "Alice", "Bob", "M67", 4,
                1, 2, 3, 4, 5, 6,
                "item.flansmod.body_armour", "none"));
    }

    @Test
    void warningIdentifiesPlayersLifetimeAndThreshold()
    {
        assertEquals(
            "Possible spawn kill: killer=Alice victim=Bob victimLifetimeSeconds=4 warningThresholdSeconds=10",
            ExplosionKillAudit.formatSpawnKillWarning("Alice", "Bob", 4, 10));
    }
}
