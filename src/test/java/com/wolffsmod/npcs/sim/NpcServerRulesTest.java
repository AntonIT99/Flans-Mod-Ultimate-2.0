package com.wolffsmod.npcs.sim;

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
}
