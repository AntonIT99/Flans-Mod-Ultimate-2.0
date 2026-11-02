package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class NpcCombatPolicyTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aBattleOverrideDoesNotOverwriteTheIndividualChoice()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        NpcWeaponOptions own = new NpcWeaponOptions();
        try
        {
            NpcCombatPolicy.overrides(entity -> NpcTacticalProfile.CAUTIOUS);
            assertFalse(NpcCombatPolicy.feature(npc, Feature.ADAPTIVE_FLANKING, own.enabled(Feature.ADAPTIVE_FLANKING)));
            assertTrue(own.enabled(Feature.ADAPTIVE_FLANKING));
        }
        finally
        {
            NpcCombatPolicy.overrides(entity -> NpcTacticalProfile.INHERIT);
        }
        assertTrue(NpcCombatPolicy.feature(npc, Feature.ADAPTIVE_FLANKING, own.enabled(Feature.ADAPTIVE_FLANKING)));
    }

    @Test
    void normalNpcsStartWithGeneralSurvivalSkillsAndBoundedMovement()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        for (Feature feature : Feature.values())
        {
            if (NpcTacticalProfile.BALANCED.feature(feature) != null)
                assertTrue(options.enabled(feature), feature.name());
        }
        NpcTuning tuning = new NpcTuning();
        assertEquals(NpcTuning.MovementFreedom.LOCAL.ordinal(), tuning.get(Value.COMBAT_MOVEMENT));
        assertEquals(24, tuning.get(Value.MANOEUVRE_RADIUS));
        assertEquals(30, tuning.get(Value.RETREAT_HEALTH));
    }

    @Test
    void explicitPerNpcOptOutsSurviveLoadingAndProfilesOnlyContainBehaviour()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.set(Feature.TACTICAL_COVER, false);
        CompoundTag saved = new CompoundTag();
        options.save(saved);
        NpcWeaponOptions restored = new NpcWeaponOptions();
        restored.load(saved);
        assertFalse(restored.enabled(Feature.TACTICAL_COVER));
        assertTrue(restored.enabled(Feature.VEHICLE_EVASION));
        assertNull(NpcTacticalProfile.AGGRESSIVE.feature(Feature.INVULNERABLE));
        assertNull(NpcTacticalProfile.AGGRESSIVE.value(Value.LAND_SPEED));
        assertNull(NpcTacticalProfile.CAUTIOUS.value(Value.AIM_ERROR));
        assertNull(NpcTacticalProfile.INHERIT.feature(Feature.TACTICAL_COVER));
    }
}
