package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.parts.PartArmor;
import com.flansmodultimate.api.projectiles.AmmunitionArmor;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.sim.NpcSight;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcVehicleCombatTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aRealVehiclesRiderIsProtectedOnlyWhenAKnownPlateActuallyBlocksTheShot()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        LivingEntity rider = mock(LivingEntity.class);
        Entity vehicle = mock(Entity.class);
        Vec3 origin = new Vec3(0D, 1D, -10D);
        Vec3 aim = new Vec3(0D, 1D, 0D);
        AABB riderBox = new AABB(-0.3D, 0D, -0.3D, 0.3D, 1.8D, 0.3D);
        Vec3 contact = riderBox.clip(origin, aim).orElseThrow();
        when(rider.getBoundingBox()).thenReturn(riderBox);
        var round = Optional.of(new AmmunitionArmor(20D, false, 12D));
        try (var equipment = mockStatic(NpcEquipment.class);
            var threats = mockStatic(NpcVehicleThreats.class);
            var states = mockStatic(NpcVehicleState.class);
            var types = mockStatic(FlansEntityTypes.class))
        {
            equipment.when(() -> NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.ADAPTIVE_VEHICLE_COMBAT)).thenReturn(true);
            threats.when(() -> NpcVehicleThreats.groundVehicle(rider)).thenReturn(true);
            threats.when(() -> NpcVehicleThreats.platform(rider)).thenReturn(vehicle);
            states.when(() -> NpcVehicleState.aimPoint(rider)).thenReturn(aim);
            types.when(() -> FlansEntityTypes.getArmorAlongRay(vehicle, origin, contact)).thenReturn(Optional.of(new PartArmor(80F, 80F)));
            assertFalse(NpcVehicleCombat.mayFire(npc, rider, origin, round, false));
            types.when(() -> FlansEntityTypes.getArmorAlongRay(vehicle, origin, contact)).thenReturn(Optional.empty());
            assertTrue(NpcVehicleCombat.mayFire(npc, rider, origin, round, false), "exposed riders and unknown plates remain eligible");
            types.when(() -> FlansEntityTypes.getArmorAlongRay(vehicle, origin, contact)).thenReturn(Optional.of(new PartArmor(10F, 10F)));
            assertTrue(NpcVehicleCombat.mayFire(npc, rider, origin, round, false));
        }
    }

    @Test
    void independentMgChoosesInfantryWhileTheMainTargetStaysArmored()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        ServerLevel level = mock(ServerLevel.class);
        LivingEntity tank = mock(LivingEntity.class);
        LivingEntity infantry = mock(LivingEntity.class);
        ISeat seat = mock(ISeat.class);
        when(npc.level()).thenReturn(level);
        when(npc.getTarget()).thenReturn(tank);
        when(npc.getBoundingBox()).thenReturn(new AABB(-1D, 0D, -1D, 1D, 2D, 1D));
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(new NpcTacticState());
        when(level.getGameTime()).thenReturn(100L);
        when(tank.isAlive()).thenReturn(true);
        when(infantry.isAlive()).thenReturn(true);
        when(seat.getGunArmor()).thenReturn(Optional.of(new AmmunitionArmor(20D, false, 12D)));
        when(level.getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any())).thenReturn(List.of(infantry));
        try (var equipment = mockStatic(NpcEquipment.class);
            var threats = mockStatic(NpcVehicleThreats.class);
            var sight = mockStatic(NpcSight.class);
            var vehicles = mockStatic(NpcVehicleState.class);
            var enemies = mockConstruction(NPCAttackSelector.class, (selector, context) -> when(selector.isEntityApplicable(infantry)).thenReturn(true)))
        {
            equipment.when(() -> NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.ADAPTIVE_VEHICLE_COMBAT)).thenReturn(true);
            threats.when(() -> NpcVehicleThreats.groundVehicle(tank)).thenReturn(true);
            threats.when(() -> NpcVehicleThreats.platform(tank)).thenReturn(tank);
            threats.when(() -> NpcVehicleThreats.protection(eq(tank), any(Vec3.class), anyString())).thenReturn(Optional.of(new PartArmor(80F, 80F)));
            vehicles.when(() -> NpcVehicleState.aimPoint(infantry)).thenReturn(new Vec3(10D, 1D, 0D));
            sight.when(() -> NpcSight.clear(eq(level), any(Vec3.class), any(Vec3.class), eq(0), eq(npc))).thenReturn(true);
            assertFalse(NpcVehicleCombat.mayFire(npc, tank, Vec3.ZERO, seat.getGunArmor(), false));
            assertSame(infantry, NpcVehicleCombat.gunnerTarget(npc, seat, Vec3.ZERO, tank, 64D));
            assertSame(tank, npc.getTarget());
            // The independent target is reused between searches, and never replaces the cannon's target.
            assertSame(infantry, NpcVehicleCombat.gunnerTarget(npc, seat, Vec3.ZERO, tank, 64D));
            verify(npc, never()).setTarget(any());
            verify(level, times(1)).getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any());
            // Unknown ammunition and armour, cannon shells and explosives retain their normal firing path.
            assertTrue(NpcVehicleCombat.mayFire(npc, tank, Vec3.ZERO, Optional.empty(), false));
            assertTrue(NpcVehicleCombat.mayFire(npc, tank, Vec3.ZERO, Optional.of(new AmmunitionArmor(20D, false, 2000D)), false));
            assertTrue(NpcVehicleCombat.mayFire(npc, tank, Vec3.ZERO, seat.getGunArmor(), true));
            threats.when(() -> NpcVehicleThreats.protection(tank, Vec3.ZERO, "turret")).thenReturn(Optional.empty());
            assertTrue(NpcVehicleCombat.mayFire(npc, tank, Vec3.ZERO, seat.getGunArmor(), false));
            assertEquals(1, enemies.constructed().size());
        }
    }
}
