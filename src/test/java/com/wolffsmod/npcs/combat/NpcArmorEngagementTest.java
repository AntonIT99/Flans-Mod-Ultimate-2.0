package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.parts.PartArmor;
import com.flansmodultimate.api.projectiles.AmmunitionArmor;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NpcArmorEngagementTest
{
    private static final AmmunitionArmor SHELL = new AmmunitionArmor(80D, false, 6800D);
    private static final AmmunitionArmor MG = new AmmunitionArmor(20D, false, 12D);
    private static final Vec3 FRONT = new Vec3(0D, 0D, -1D);

    @Test
    void threeConfirmedFrontalShellFailuresStartFlanking()
    {
        NpcArmorEngagement state = new NpcArmorEngagement();
        state.impact(1, "core", FRONT, SHELL, true, 100L);
        state.impact(1, "turret", FRONT, SHELL, true, 140L);
        assertFalse(state.flanking(1, 140L));
        state.impact(1, "core", FRONT, SHELL, true, 180L);
        assertTrue(state.flanking(1, 180L));
    }

    @Test
    void mgHitsOverlappingBoxesAndNonFrontalHitsDoNotArtificiallyTriggerFlanking()
    {
        NpcArmorEngagement state = new NpcArmorEngagement();
        for (long tick = 100L; tick < 200L; tick += 20L)
        {
            state.impact(1, "core", FRONT, MG, true, tick);
            state.impact(1, "leftTrack", FRONT, SHELL, true, tick);
            state.impact(1, "turret", new Vec3(1D, 0D, 0D), SHELL, true, tick);
        }
        assertFalse(state.flanking(1, 200L));
        state.impact(1, "core", FRONT, SHELL, true, 200L);
        state.impact(1, "turret", FRONT, SHELL, true, 200L);
        state.impact(1, "core", FRONT, SHELL, true, 201L);
        assertFalse(state.flanking(1, 201L));
    }

    @Test
    void penetrationTargetChangesAndExpiryClearFailuresAndWaypoints()
    {
        for (int reset = 0; reset < 3; reset++)
        {
            NpcArmorEngagement state = failedEngagement();
            state.setWaypoint(new Vec3(1D, 0D, 0D));
            state.setSide(1);
            if (reset == 0)
                state.impact(1, "turret", FRONT, SHELL, false, 200L);
            assertFalse(state.flanking(reset == 1 ? 2 : 1, reset == 2 ? 800L : 200L));
            assertNull(state.getWaypoint());
            assertEquals(0, state.getSide());
        }
    }

    @Test
    void unspecifiedMassUsesPenetrationButKnownCannonRoundsAndHeatAreNeverMachineGuns()
    {
        assertTrue(NpcArmorEngagement.smallArms(MG));
        assertTrue(NpcArmorEngagement.smallArms(new AmmunitionArmor(80D, false, 45D)), "heavy MG ammunition still counts as small arms");
        assertTrue(NpcArmorEngagement.smallArms(new AmmunitionArmor(10D, false)));
        assertFalse(NpcArmorEngagement.smallArms(new AmmunitionArmor(80D, false)));
        assertFalse(NpcArmorEngagement.smallArms(new AmmunitionArmor(10D, false, 2000D)), "low-penetration shells still get their trial shots");
        assertFalse(NpcArmorEngagement.smallArms(new AmmunitionArmor(80D, true, 90D)));
    }

    @Test
    void bothKnownPlatesMustStopTheRoundAndEqualityStillPenetrates()
    {
        Optional<PartArmor> heavy = Optional.of(new PartArmor(80F, 120F));
        Optional<PartArmor> light = Optional.of(new PartArmor(10F, 10F));
        assertTrue(NpcVehicleCombat.blocked(MG, heavy, heavy));
        assertFalse(NpcVehicleCombat.blocked(MG, heavy, light));
        assertFalse(NpcVehicleCombat.blocked(MG, light, heavy));
        assertFalse(NpcVehicleCombat.blocked(MG, heavy, Optional.empty()));
        assertFalse(NpcVehicleCombat.blocked(MG, Optional.empty(), heavy));
        assertFalse(NpcVehicleCombat.blocked(SHELL, heavy, heavy));
        assertTrue(NpcVehicleCombat.blocked(new AmmunitionArmor(80D, true), heavy, heavy));
    }

    @Test
    void arcStepsPreserveRangeAndHeightWhileReachingTheSide()
    {
        Vec3 target = new Vec3(10D, 3D, 20D);
        Vec3 start = target.add(0D, 2D, 24D);
        Vec3 point = start;
        for (int step = 0; step < 4; step++)
            point = NpcVehicleCombat.flankStep(point, target, Math.PI / 8D);
        assertEquals(start.y, point.y, 1.0E-6D);
        assertEquals(24D, point.subtract(target).horizontalDistance(), 1.0E-5D);
        assertEquals(0D, point.z - target.z, 1.0E-5D);
        assertTrue(point.x > target.x + 23D);
    }

    @Test
    void adaptiveVehicleCombatIsEnabledForOrdinaryNpcsWithoutATeamsProfile()
    {
        assertTrue(new NpcWeaponOptions().enabled(NpcWeaponOptions.Feature.ADAPTIVE_VEHICLE_COMBAT));
    }

    private static NpcArmorEngagement failedEngagement()
    {
        NpcArmorEngagement state = new NpcArmorEngagement();
        for (long tick = 100L; tick <= 180L; tick += 40L)
            state.impact(1, "core", FRONT, SHELL, true, tick);
        assertTrue(state.flanking(1, 180L));
        return state;
    }
}
