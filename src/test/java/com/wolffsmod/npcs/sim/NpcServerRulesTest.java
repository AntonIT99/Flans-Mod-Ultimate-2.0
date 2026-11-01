package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.diag.NpcServerSettings.AmmoPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcServerRulesTest
{
    @Test
    void theServersAmmunitionRuleOutranksTheNpcsSwitchUnlessItLeavesItToEachNpc()
    {
        assertTrue(NpcServerRules.consumesAmmo(AmmoPolicy.PER_NPC, true));
        assertFalse(NpcServerRules.consumesAmmo(AmmoPolicy.PER_NPC, false));
        assertFalse(NpcServerRules.consumesAmmo(AmmoPolicy.NEVER, true));
        assertTrue(NpcServerRules.consumesAmmo(AmmoPolicy.ALWAYS, false));
        // Off the server thread there is no rule to apply.
        assertTrue(NpcServerRules.consumesAmmo((AmmoPolicy) null, true));
    }

    @Test
    void cinematicModeLeavesPlayersHalfAHeartAndIsOffUnlessAnOperatorTurnsItOn()
    {
        assertEquals(0, NpcServerSettings.Setting.CINEMATIC.getFallback());
        assertEquals(4F, NpcServerRules.spared(20F, 4F), 1.0E-6F);
        assertEquals(5F, NpcServerRules.spared(6F, 50F), 1.0E-6F);
        assertEquals(0F, NpcServerRules.spared(0.5F, 3F), 1.0E-6F);
    }
}
