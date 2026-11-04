package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.parts.PartArmor;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.teams.TeamsBots;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcInfantryAirObservationTest
{
    @Test
    void aPilotInASeparateSeatIsClassifiedByThePhysicalAircraftAndItsAltitude()
    {
        LivingEntity pilot = mock(LivingEntity.class);
        Entity aircraft = mock(Entity.class);
        IDriveableType type = mock(IDriveableType.class);
        when(type.getDriveableKind()).thenReturn(DriveableKind.PLANE);
        try (var bodies = mockStatic(NpcVehicleThreats.class); var types = mockStatic(FlansEntityTypes.class); var altitude = mockStatic(NpcBombTargets.class))
        {
            bodies.when(() -> NpcVehicleThreats.platform(pilot)).thenReturn(aircraft);
            types.when(() -> FlansEntityTypes.getDriveableType(aircraft)).thenReturn(Optional.of(type));
            altitude.when(() -> NpcBombTargets.airborne(aircraft)).thenReturn(true);
            assertTrue(NpcAirCombat.airborne(pilot));
            altitude.when(() -> NpcBombTargets.airborne(aircraft)).thenReturn(false);
            assertFalse(NpcAirCombat.airborne(pilot));
            when(type.isShip()).thenReturn(true);
            assertFalse(NpcAirCombat.aircraft(pilot));
        }
    }

    @Test
    void equippedApproachStopsInsteadOfChasingAnAirborneTarget()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        LivingEntity target = mock(LivingEntity.class);
        PathNavigation navigation = mock(PathNavigation.class);
        when(npc.getTarget()).thenReturn(target);
        when(npc.getNavigation()).thenReturn(navigation);
        try (var air = mockStatic(NpcAirCombat.class); var kinds = mockStatic(TacticKind.class))
        {
            air.when(() -> NpcAirCombat.airborne(target)).thenReturn(true);
            kinds.when(() -> TacticKind.of(npc)).thenReturn(TacticKind.STANDARD);
            new NpcCombatApproachGoal(npc).tick();
            verify(navigation).stop();
            verify(navigation, never()).moveTo(any(Entity.class), anyDouble());
        }
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void onlyAVisibleHostileApproachCreatesAWarningAndSupportRequest()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class, withSettings().extraInterfaces(NpcTacticState.Holder.class));
        NpcTacticState state = new NpcTacticState();
        when(((NpcTacticState.Holder) npc).wolffsmodnpcsTacticState()).thenReturn(state);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        when(npc.position()).thenReturn(Vec3.ZERO);
        Mob aircraft = mock(Mob.class);
        Vec3 position = new Vec3(0D, 30D, -40D);
        when(aircraft.position()).thenReturn(position);
        when(aircraft.getEyePosition()).thenReturn(position);
        when(aircraft.getX()).thenReturn(0D);
        when(aircraft.getY()).thenReturn(30D);
        when(aircraft.getZ()).thenReturn(-40D);
        aircraft.xo = 0D;
        aircraft.yo = 30D;
        aircraft.zo = -41D;
        when(aircraft.isAlive()).thenReturn(true);
        when(aircraft.getTarget()).thenReturn(npc);
        Sensing sensing = mock(Sensing.class);
        when(npc.getSensing()).thenReturn(sensing);
        NpcCombatants combatants = mock(NpcCombatants.class);
        when(combatants.size()).thenReturn(1);
        when(combatants.get(0)).thenReturn(new NpcCombatants.Combatant(aircraft, null, new AABB(position, position.add(1D, 1D, 1D)), -1));
        try (var equipment = mockStatic(NpcEquipment.class); var kinds = mockStatic(TacticKind.class); var bodies = mockStatic(NpcVehicleThreats.class);
            var air = mockStatic(NpcAirCombat.class); var relations = mockStatic(NpcDrivers.class); var loaded = mockStatic(NpcCombatants.class); var teams = mockStatic(TeamsBots.class))
        {
            kinds.when(() -> TacticKind.of(npc)).thenReturn(TacticKind.STANDARD);
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.TACTICAL_COVER)).thenReturn(true);
            bodies.when(() -> NpcVehicleThreats.platform(aircraft)).thenReturn(aircraft);
            air.when(() -> NpcAirCombat.airborne(aircraft)).thenReturn(true);
            loaded.when(() -> NpcCombatants.of(level)).thenReturn(combatants);
            relations.when(() -> NpcDrivers.hostile(npc, aircraft)).thenReturn(true);
            NpcInfantryAirDefense.observe(npc);
            assertFalse(state.getSafety().getAirDefense().active(0L));
            teams.verifyNoInteractions();
            when(level.getGameTime()).thenReturn(20L);
            when(sensing.hasLineOfSight(aircraft)).thenReturn(true);
            NpcInfantryAirDefense.observe(npc);
            assertTrue(state.getSafety().getAirDefense().active(20L));
            assertEquals(40L, state.getSafety().getUrgentUntil());
            teams.verify(() -> TeamsBots.reportAirAttack(npc, aircraft));
            var snapshot = state.getSafety().getAirDefense().getSources();
            when(level.getGameTime()).thenReturn(40L);
            when(sensing.hasLineOfSight(aircraft)).thenReturn(false);
            when(aircraft.position()).thenReturn(new Vec3(0D, 30D, -5D));
            NpcInfantryAirDefense.observe(npc);
            assertSame(snapshot, state.getSafety().getAirDefense().getSources());
            assertEquals(120L, state.getSafety().getAirDefense().getUntil());
            when(level.getGameTime()).thenReturn(120L);
            assertFalse(state.getSafety().getAirDefense().active(120L));
            relations.when(() -> NpcDrivers.hostile(npc, aircraft)).thenReturn(false);
            when(sensing.hasLineOfSight(aircraft)).thenReturn(true);
            NpcInfantryAirDefense.observe(npc);
            assertFalse(state.getSafety().getAirDefense().active(120L), "friendly aircraft do not cause an air warning");
        }
    }

    @Test
    void aReadableWeaponNeedsAUsableInterceptAndEnoughPenetrationWhileUnknownArmorStaysEligible()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        LivingEntity aircraft = mock(LivingEntity.class);
        ItemStack rifle = new ItemStack(Items.BLAZE_ROD);
        ItemStack ammo = new ItemStack(Items.IRON_NUGGET);
        ProjectileShot shot = mock(ProjectileShot.class);
        when(shot.speed()).thenReturn(5F);
        when(shot.drag()).thenReturn(0.99D);
        when(npc.position()).thenReturn(Vec3.ZERO);
        when(npc.getEyePosition()).thenReturn(Vec3.ZERO);
        when(aircraft.getEyePosition()).thenReturn(new Vec3(0D, 25D, 60D));
        when(aircraft.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(npc.isInRange(eq(aircraft), anyDouble())).thenReturn(true);
        try (var equipment = mockStatic(NpcEquipment.class); var kinds = mockStatic(TacticKind.class); var bodies = mockStatic(NpcVehicleThreats.class);
            var air = mockStatic(NpcAirCombat.class); var items = mockStatic(FlansEquipment.class); var rounds = mockStatic(FlansProjectiles.class);
            var choice = mockStatic(NpcWeaponChoice.class); var ranges = mockStatic(NpcWeaponRange.class))
        {
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.SUITED_TARGETS)).thenReturn(true);
            equipment.when(() -> NpcEquipment.inherits(eq(npc), any())).thenReturn(true);
            kinds.when(() -> TacticKind.of(npc)).thenReturn(TacticKind.STANDARD);
            bodies.when(() -> NpcVehicleThreats.platform(aircraft)).thenReturn(aircraft);
            air.when(() -> NpcAirCombat.airborne(aircraft)).thenReturn(true);
            choice.when(() -> NpcWeaponChoice.ammunition(npc, rifle)).thenReturn(ammo);
            items.when(() -> FlansEquipment.getWeaponProperties(rifle, ammo)).thenReturn(Optional.of(mock(EquippedWeaponProperties.class)));
            items.when(() -> FlansEquipment.getArmorPenetration(rifle, ammo)).thenReturn(OptionalDouble.of(8D));
            rounds.when(() -> FlansProjectiles.prepare(eq(npc), eq(ammo), eq(rifle), isNull(), isNull(), eq(false), any())).thenReturn(Optional.of(shot));
            assertFalse(NpcInfantryAirDefense.holdsFire(npc, aircraft, rifle));
            bodies.when(() -> NpcVehicleThreats.protection(aircraft, Vec3.ZERO)).thenReturn(Optional.of(new PartArmor(30F, 30F)));
            assertTrue(NpcInfantryAirDefense.holdsFire(npc, aircraft, rifle));
            items.when(() -> FlansEquipment.getArmorPenetration(rifle, ammo)).thenReturn(OptionalDouble.of(40D));
            assertFalse(NpcInfantryAirDefense.holdsFire(npc, aircraft, rifle));
            when(shot.dropped()).thenReturn(true);
            assertTrue(NpcInfantryAirDefense.holdsFire(npc, aircraft, rifle));
            when(shot.dropped()).thenReturn(false);
            when(npc.isInRange(eq(aircraft), anyDouble())).thenReturn(false);
            assertTrue(NpcInfantryAirDefense.holdsFire(npc, aircraft, rifle));
            when(npc.isPassenger()).thenReturn(true);
            assertFalse(NpcInfantryAirDefense.holdsFire(npc, aircraft, rifle), "mounted gunners keep their own weapon policy");
            when(npc.isPassenger()).thenReturn(false);
            air.when(() -> NpcAirCombat.airborne(aircraft)).thenReturn(false);
            assertFalse(NpcInfantryAirDefense.holdsFire(npc, aircraft, rifle), "landed aircraft remain ordinary targets");
        }
    }
}
