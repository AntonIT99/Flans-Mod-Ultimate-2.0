package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.WeaponKind;
import com.flansmodultimate.api.projectiles.DetonationRadii;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.tactics.NpcWeaponRange;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcBlastSafetyTest
{
    private static final double MARGIN = 4D;
    private static final double RANGE = 64D;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void anEquippedLauncherKeepsItsBlastDistanceDuringReloadAndUsesItsEffectiveRange()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        when(npc.getHealth()).thenReturn(20F);
        when(npc.getMaxHealth()).thenReturn(20F);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcWeaponOptions options = new NpcWeaponOptions();
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning()).thenReturn(new NpcTuning());
        ItemStack launcher = new ItemStack(Items.BLAZE_ROD);
        ItemStack rocket = new ItemStack(Items.FIRE_CHARGE);
        try (var equipment = mockStatic(NpcEquipment.class);
            var flan = mockStatic(FlansEquipment.class);
            var ammunition = mockStatic(NpcWeaponChoice.class);
            var projectiles = mockStatic(FlansProjectiles.class);
            var range = mockStatic(NpcWeaponRange.class);
            var kinds = mockStatic(TacticKind.class))
        {
            kinds.when(() -> TacticKind.of(npc)).thenReturn(TacticKind.STANDARD);
            equipment.when(() -> NpcEquipment.ranged(npc)).thenReturn(true);
            equipment.when(() -> NpcEquipment.rangedWeapon(npc)).thenReturn(launcher);
            flan.when(() -> FlansEquipment.getWeaponKind(launcher)).thenReturn(Optional.of(new WeaponKind(true, false, false)));
            ammunition.when(() -> NpcWeaponChoice.ammunition(npc, launcher)).thenReturn(rocket);
            projectiles.when(() -> FlansProjectiles.getDetonationRadii(rocket)).thenReturn(Optional.of(new DetonationRadii(6D, 0D, 0D, 0D)));
            range.when(() -> NpcWeaponRange.firing(npc)).thenReturn(64);
            assertEquals(6D, NpcBlastSafety.equippedWeaponReach(npc));
            assertEquals(5.1D, NpcBlastSafety.standoff(npc, NpcBlastSafety.equippedWeaponReach(npc)), 1.0E-9D);
            when(npc.getHealth()).thenReturn(10F);
            assertEquals(5.7D, NpcBlastSafety.standoff(npc, 6D), 1.0E-9D);
            LivingEntity target = mock(LivingEntity.class);
            when(npc.distanceTo(target)).thenReturn(5.8F);
            assertFalse(NpcBlastSafety.tooClose(npc, target, 6D), "even wounded infantry may fire just inside the hazard boundary");
            when(npc.distanceTo(target)).thenReturn(5.5F);
            assertTrue(NpcBlastSafety.tooClose(npc, target, 6D));
            when(npc.getHealth()).thenReturn(20F);
            assertFalse(NpcBlastSafety.tooClose(npc, target, 6D), "healthy infantry accept a little more splash");
            when(npc.getHealth()).thenReturn(20F);
            kinds.when(() -> TacticKind.of(npc)).thenReturn(TacticKind.VEHICLE);
            assertEquals(10D, NpcBlastSafety.standoff(npc, 6D), "vehicle crews retain their full safety margin");
            options.set(NpcWeaponOptions.Feature.BLAST_STANDOFF, false);
            assertEquals(0D, NpcBlastSafety.standoff(npc, 6D));
        }
    }

    @Test
    void infantryBecomeMoreCautiousAsTheyLoseHealth()
    {
        assertEquals(5.1D, NpcBlastSafety.infantryStandoff(6D, MARGIN, RANGE, 1D), 1.0E-9D);
        assertEquals(5.1D, NpcBlastSafety.infantryStandoff(6D, MARGIN, RANGE, 0.75D), 1.0E-9D);
        assertEquals(5.4D, NpcBlastSafety.infantryStandoff(6D, MARGIN, RANGE, 0.625D), 1.0E-9D);
        assertEquals(5.7D, NpcBlastSafety.infantryStandoff(6D, MARGIN, RANGE, 0.5D), 1.0E-9D);
        assertEquals(5.7D, NpcBlastSafety.infantryStandoff(6D, MARGIN, RANGE, 0.1D), 1.0E-9D);
        assertEquals(5.7D, NpcBlastSafety.infantryStandoff(6D, 100D, RANGE, 0.1D), 1.0E-9D, "the margin cannot demand zero splash from infantry");
        assertEquals(0D, NpcBlastSafety.infantryStandoff(0D, MARGIN, RANGE, 1D));
        assertEquals(0D, NpcBlastSafety.infantryStandoff(6D, MARGIN, 0D, 1D));
        assertEquals(RANGE * NpcBlastSafety.MAX_RANGE_SHARE, NpcBlastSafety.infantryStandoff(100D, MARGIN, RANGE, 1D));
    }

    @Test
    void theReachOfARoundIsTheFurthestOfItsEffectsWithFragmentsCountedInPart()
    {
        assertEquals(6D, NpcBlastSafety.reach(new DetonationRadii(6D, 3D, 4D, 2D)));
        assertEquals(7D, NpcBlastSafety.reach(new DetonationRadii(3D, 7D, 4D, 2D)));
        assertEquals(9D, NpcBlastSafety.reach(new DetonationRadii(3D, 7D, 4D, 9D)), "fire reaches furthest");
        // Fragments that far off seldom hit, so only part of their radius is kept out of.
        assertEquals(20D * NpcBlastSafety.FRAGMENT_SHARE, NpcBlastSafety.reach(new DetonationRadii(3D, 7D, 20D, 2D)));
    }

    @Test
    void aRoundThatDoesNotGoOffKeepsNoStandoffAtAll()
    {
        assertEquals(0D, NpcBlastSafety.reach(new DetonationRadii(0D, 0D, 0D, 0D)));
        assertEquals(0D, NpcBlastSafety.standoff(0D, MARGIN, RANGE));
        assertEquals(0D, NpcBlastSafety.standoff(-1D, MARGIN, RANGE), "nothing is read as no blast either");
    }

    @Test
    void theStandoffIsTheRoundsReachPlusTheMargin()
    {
        assertEquals(10D, NpcBlastSafety.standoff(6D, MARGIN, RANGE));
        assertEquals(6D, NpcBlastSafety.standoff(6D, 0D, RANGE));
        assertEquals(6D, NpcBlastSafety.standoff(6D, -2D, RANGE), "a margin below zero adds nothing");
    }

    @Test
    void aBlastThatOutreachesTheFiringRangeStillLeavesRoomToFire()
    {
        double capped = RANGE * NpcBlastSafety.MAX_RANGE_SHARE;
        assertEquals(capped, NpcBlastSafety.standoff(80D, MARGIN, RANGE));
        assertEquals(capped, NpcBlastSafety.standoff(RANGE, MARGIN, RANGE));
        assertEquals(0D, NpcBlastSafety.standoff(6D, MARGIN, 0D), "with no range at all it holds nothing back");
    }
}
