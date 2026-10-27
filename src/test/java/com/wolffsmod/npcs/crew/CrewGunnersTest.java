package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.ReloadCycle;
import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcAmmunition;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrewGunnersTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void mannedGunsFireInventoryAmmoWhilePassengersOwnWeaponsAreDisabled()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        EntityNPCInterface gunner = mock(EntityNPCInterface.class);
        LivingEntity target = mock(LivingEntity.class);
        ServerLevel level = mock(ServerLevel.class, RETURNS_DEEP_STUBS);
        when(vehicle.level()).thenReturn(level);
        when(target.level()).thenReturn(level);
        when(vehicle.isAlive()).thenReturn(true);
        when(target.isAlive()).thenReturn(true);
        when(vehicle.getTarget()).thenReturn(target);
        vehicle.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        vehicle.stats.ranged = mock(DataRanged.class);
        when(vehicle.stats.ranged.getRange()).thenReturn(100);
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.set(Feature.MANNED_GUNS_FIRE, true);
        options.set(Feature.PASSENGER_GUNS, true);
        options.set(Feature.FIRE_WHEN_ALIGNED, false);
        options.set(Feature.FIRE_SOUNDS, false);
        options.set(Feature.SHOOT_PARTICLES, false);
        when(((NpcWeaponSettings) vehicle.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        NpcCrewSettings settings = new NpcCrewSettings();
        when(((NpcWeaponSettings) vehicle.stats).wolffsmodnpcsCrew()).thenReturn(settings);
        assertFalse(NpcCrewSettings.of(vehicle).isPassengersFire());
        Vec3 direction = new Vec3(0D, 0D, -1D);
        NpcVehicleState state = mock(NpcVehicleState.class, invocation -> invocation.getMethod().getReturnType() == Vec3.class ? direction : RETURNS_DEEP_STUBS.answer(invocation));
        when(state.turretAim()).thenReturn(MountAim.REST);
        IContentType definition = mock(IContentType.class);
        IContentType weapon = mock(IContentType.class);
        FlanModelEntity model = mock(FlanModelEntity.class);
        when(model.getInfoType()).thenReturn(definition);
        WeaponMount mount = new WeaponMount(1, false, false, new MountAim(-180F, -90F), new MountAim(180F, 90F), 10F, 10F, Vec3.ZERO, Vec3.ZERO, Optional.of(weapon));
        when(state.crewSeats(vehicle)).thenReturn(List.of(mount));
        ItemStack ammo = new ItemStack(Items.IRON_NUGGET);
        WeaponMuzzle muzzle = new WeaponMuzzle(Vec3.ZERO, Optional.of(weapon), List.of());
        ProjectileShot shot = mock(ProjectileShot.class);
        when(shot.speed()).thenReturn(Float.NaN);
        Entity projectile = mock(Entity.class);
        when(shot.launch(Vec3.ZERO, direction, false)).thenReturn(List.of(projectile));
        // The line-of-sight query returns the vanilla miss result.
        when(level.clip(any())).thenReturn(net.minecraft.world.phys.BlockHitResult.miss(new Vec3(0D, 0D, -10D), net.minecraft.core.Direction.NORTH, net.minecraft.core.BlockPos.ZERO));
        when(vehicle.getBoundingBox()).thenReturn(new AABB(-1D, 0D, -1D, 1D, 2D, 1D));
        when(level.getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any())).thenAnswer(invocation -> new ArrayList<>());
        try (var models = mockStatic(NpcModels.class);
            var states = mockStatic(NpcVehicleState.class);
            var crew = mockStatic(NpcCrew.class);
            var ammunition = mockStatic(NpcAmmunition.class);
            var types = mockStatic(FlansEntityTypes.class);
            var projectiles = mockStatic(FlansProjectiles.class))
        {
            models.when(() -> NpcModels.flan(vehicle)).thenReturn(model);
            models.when(() -> NpcModels.world(eq(vehicle), eq(model), any(), anyFloat())).thenReturn(Vec3.ZERO);
            states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
            states.when(() -> NpcVehicleState.aimPoint(target)).thenReturn(new Vec3(0D, 0D, -10D));
            crew.when(() -> NpcCrew.occupant(vehicle, 1)).thenReturn(Optional.of(gunner));
            ammunition.when(() -> NpcAmmunition.crewGunTemplate(vehicle, gunner, weapon)).thenReturn(ammo);
            types.when(() -> FlansEntityTypes.getSeatReloadCycle(definition, 1, ammo)).thenReturn(Optional.of(new ReloadCycle(2, 2D, 4D, OptionalInt.empty())));
            projectiles.when(() -> FlansProjectiles.getSeatMuzzle(eq(definition), eq(1), anyInt(), eq(MountAim.REST), any())).thenReturn(Optional.of(muzzle));
            projectiles.when(() -> FlansProjectiles.prepare(eq(gunner), eq(ammo), eq(ItemStack.EMPTY), eq(weapon), isNull(), eq(false), any())).thenReturn(Optional.of(shot));
            CrewGunners guns = new CrewGunners();
            guns.tick(vehicle);
            verify(shot).launch(Vec3.ZERO, direction, false);
            options.set(Feature.MANNED_GUNS_FIRE, false);
            guns.tick(vehicle);
            guns.tick(vehicle);
            verify(shot, times(1)).launch(Vec3.ZERO, direction, false);
            options.set(Feature.MANNED_GUNS_FIRE, true);
            crew.when(() -> NpcCrew.occupant(vehicle, 1)).thenReturn(Optional.empty());
            guns.tick(vehicle);
            verify(shot, times(1)).launch(Vec3.ZERO, direction, false);
        }
    }

    @Test
    void anEmptySeatsGunFiresAsTheVehicleOnlyWhileUnmannedGunsFireAndNobodySitsThere()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        LivingEntity target = mock(LivingEntity.class);
        ServerLevel level = mock(ServerLevel.class, RETURNS_DEEP_STUBS);
        when(vehicle.level()).thenReturn(level);
        when(target.level()).thenReturn(level);
        when(vehicle.isAlive()).thenReturn(true);
        when(target.isAlive()).thenReturn(true);
        when(vehicle.getTarget()).thenReturn(target);
        when(vehicle.getBoundingBox()).thenReturn(new AABB(-1D, 0D, -1D, 1D, 2D, 1D));
        vehicle.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        vehicle.stats.ranged = mock(DataRanged.class);
        when(vehicle.stats.ranged.getRange()).thenReturn(100);
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.set(Feature.FIRE_WHEN_ALIGNED, false);
        options.set(Feature.FIRE_SOUNDS, false);
        options.set(Feature.SHOOT_PARTICLES, false);
        when(((NpcWeaponSettings) vehicle.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        Vec3 direction = new Vec3(0D, 0D, -1D);
        NpcVehicleState state = mock(NpcVehicleState.class, invocation -> invocation.getMethod().getReturnType() == Vec3.class ? direction : RETURNS_DEEP_STUBS.answer(invocation));
        when(state.turretAim()).thenReturn(MountAim.REST);
        IContentType definition = mock(IContentType.class);
        IContentType weapon = mock(IContentType.class);
        FlanModelEntity model = mock(FlanModelEntity.class);
        when(model.getInfoType()).thenReturn(definition);
        WeaponMount mount = new WeaponMount(1, false, false, new MountAim(-180F, -90F), new MountAim(180F, 90F), 10F, 10F, Vec3.ZERO, Vec3.ZERO, Optional.of(weapon));
        when(state.crewSeats(vehicle)).thenReturn(List.of(mount));
        ItemStack ammo = new ItemStack(Items.IRON_NUGGET);
        ProjectileShot shot = mock(ProjectileShot.class);
        when(shot.speed()).thenReturn(Float.NaN);
        when(shot.launch(Vec3.ZERO, direction, false)).thenReturn(List.of(mock(Entity.class)));
        when(level.clip(any())).thenReturn(net.minecraft.world.phys.BlockHitResult.miss(new Vec3(0D, 0D, -10D), net.minecraft.core.Direction.NORTH, net.minecraft.core.BlockPos.ZERO));
        when(level.getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any())).thenAnswer(invocation -> new ArrayList<>());
        try (var models = mockStatic(NpcModels.class);
            var states = mockStatic(NpcVehicleState.class);
            var crew = mockStatic(NpcCrew.class);
            var ammunition = mockStatic(NpcAmmunition.class);
            var types = mockStatic(FlansEntityTypes.class);
            var projectiles = mockStatic(FlansProjectiles.class))
        {
            models.when(() -> NpcModels.flan(vehicle)).thenReturn(model);
            models.when(() -> NpcModels.world(eq(vehicle), eq(model), any(), anyFloat())).thenReturn(Vec3.ZERO);
            states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
            states.when(() -> NpcVehicleState.aimPoint(target)).thenReturn(new Vec3(0D, 0D, -10D));
            crew.when(() -> NpcCrew.occupant(vehicle, 1)).thenReturn(Optional.empty());
            ammunition.when(() -> NpcAmmunition.crewGunTemplate(vehicle, vehicle, weapon)).thenReturn(ammo);
            types.when(() -> FlansEntityTypes.getSeatReloadCycle(definition, 1, ammo)).thenReturn(Optional.of(new ReloadCycle(2, 2D, 4D, OptionalInt.empty())));
            projectiles.when(() -> FlansProjectiles.getSeatMuzzle(eq(definition), eq(1), anyInt(), eq(MountAim.REST), any()))
                .thenReturn(Optional.of(new WeaponMuzzle(Vec3.ZERO, Optional.of(weapon), List.of())));
            projectiles.when(() -> FlansProjectiles.prepare(eq(vehicle), eq(ammo), eq(ItemStack.EMPTY), eq(weapon), isNull(), eq(false), any())).thenReturn(Optional.of(shot));
            CrewGunners guns = new CrewGunners();
            // Off, as it starts, an empty seat's gun stays silent.
            guns.tick(vehicle);
            verify(shot, never()).launch(any(), any(), anyBoolean());
            options.set(Feature.UNMANNED_GUNS_FIRE, true);
            guns.tick(vehicle);
            verify(shot, times(1)).launch(Vec3.ZERO, direction, false);
            // A rider who is no crew member, such as a player, keeps the gun silent.
            crew.when(() -> NpcCrew.occupied(vehicle, 1)).thenReturn(true);
            for (int tick = 0; tick < 10; tick++)
                guns.tick(vehicle);
            verify(shot, times(1)).launch(Vec3.ZERO, direction, false);
        }
    }

    @Test
    void aGunThatCannotTurnOntoTheVehiclesTargetPicksAnEnemyWithinItsOwnArc()
    {
        // A hull machine gun that turns 30 degrees either way; the vehicle's target lies off to its side, another enemy ahead.
        List<Vec3> own = ledAt(true, new Vec3(10D, 0D, 0D), new Vec3(0D, 0D, 10D));
        assertEquals(1, own.size());
        assertTrue(own.get(0).z > 0.99D);
        // Without gunners picking their own targets, the gun waits on the vehicle's target, which it cannot reach.
        assertTrue(ledAt(false, new Vec3(10D, 0D, 0D), new Vec3(0D, 0D, 10D)).stream().allMatch(lead -> lead.x > 0.99D));
    }

    @Test
    void eachGunTakesTheNearestEnemyItReachesWhetherOrNotItIsTheVehiclesTarget()
    {
        // The vehicle's target is within the gun's arc but farther than another enemy: the gun takes the nearer one.
        List<Vec3> nearer = ledAt(true, new Vec3(0D, 0D, 20D), new Vec3(2D, 0D, 8D));
        assertEquals(1, nearer.size());
        assertTrue(nearer.get(0).x > 0.2D);
        // The vehicle's target is not avoided either: nearest, it is the gun's target too.
        List<Vec3> shared = ledAt(true, new Vec3(0D, 0D, 6D), new Vec3(2D, 0D, 15D));
        assertEquals(1, shared.size());
        assertTrue(shared.get(0).z > 0.99D);
    }

    @Test
    void aGunSwitchesAtOnceWhenTheVehiclesMovementTakesItsTargetOutOfReach()
    {
        // Facing +Z the gun takes the nearer vehicle's target; the hull then swings 40 degrees, which leaves that target out of
        // the gun's 30 degree arc but brings the other enemy within it, long before the gun would look again by itself.
        List<Vec3> leads = ledAt(true, new Vec3(0D, 0D, 6D), new Vec3(-5D, 0D, 10D), 0F, 40F);
        assertEquals(2, leads.size());
        assertTrue(leads.get(0).z > 0.99D);
        assertTrue(leads.get(1).x < -0.4D);
    }

    /**
     * @param targetPoint
     *            where the vehicle's target is aimed at, the vehicle standing at the origin facing +Z
     * @param otherPoint
     *            where another enemy is aimed at
     * @param hullYaws
     *            where the hull faces on each tick run, one tick facing +Z without any
     * @return the directions the hull gun was laid along over the ticks
     */
    private static List<Vec3> ledAt(boolean ownTargets, Vec3 targetPoint, Vec3 otherPoint, float... hullYaws)
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        EntityNPCInterface gunner = mock(EntityNPCInterface.class);
        LivingEntity aside = mock(LivingEntity.class);
        LivingEntity ahead = mock(LivingEntity.class);
        when(vehicle.distanceToSqr(aside)).thenReturn(targetPoint.lengthSqr());
        when(vehicle.distanceToSqr(ahead)).thenReturn(otherPoint.lengthSqr());
        ServerLevel level = mock(ServerLevel.class, RETURNS_DEEP_STUBS);
        when(vehicle.level()).thenReturn(level);
        when(vehicle.getRootVehicle()).thenReturn(vehicle);
        when(vehicle.getBoundingBox()).thenReturn(new AABB(-1D, 0D, -1D, 1D, 2D, 1D));
        when(vehicle.isAlive()).thenReturn(true);
        when(vehicle.getTarget()).thenReturn(aside);
        for (LivingEntity enemy : List.of(aside, ahead))
        {
            when(enemy.level()).thenReturn(level);
            when(enemy.isAlive()).thenReturn(true);
            when(enemy.getRootVehicle()).thenReturn(enemy);
        }
        vehicle.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        vehicle.stats.ranged = mock(DataRanged.class);
        when(vehicle.stats.ranged.getRange()).thenReturn(100);
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.set(Feature.FIRE_WHEN_ALIGNED, false);
        options.set(Feature.FIRE_SOUNDS, false);
        options.set(Feature.SHOOT_PARTICLES, false);
        options.set(Feature.GUNNER_TARGETS, ownTargets);
        when(((NpcWeaponSettings) vehicle.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        NpcVehicleState state = mock(NpcVehicleState.class, RETURNS_DEEP_STUBS);
        when(state.turretAim()).thenReturn(MountAim.REST);
        IContentType definition = mock(IContentType.class);
        IContentType weapon = mock(IContentType.class);
        FlanModelEntity model = mock(FlanModelEntity.class);
        when(model.getInfoType()).thenReturn(definition);
        WeaponMount mount = new WeaponMount(1, false, false, new MountAim(-30F, -10F), new MountAim(30F, 10F), 10F, 10F, Vec3.ZERO, Vec3.ZERO, Optional.of(weapon));
        when(state.crewSeats(vehicle)).thenReturn(List.of(mount));
        ItemStack ammo = new ItemStack(Items.IRON_NUGGET);
        when(level.clip(any())).thenReturn(net.minecraft.world.phys.BlockHitResult.miss(Vec3.ZERO, net.minecraft.core.Direction.NORTH, net.minecraft.core.BlockPos.ZERO));
        when(level.getEntitiesOfClass(eq(LivingEntity.class), any(AABB.class), any())).thenAnswer(invocation -> new ArrayList<>(List.of(ahead)));
        List<Vec3> leads = new ArrayList<>();
        doAnswer(invocation -> leads.add(invocation.getArgument(1))).when(state).leadSeat(eq(1), any());
        try (var models = mockStatic(NpcModels.class);
            var states = mockStatic(NpcVehicleState.class);
            var crew = mockStatic(NpcCrew.class);
            var ammunition = mockStatic(NpcAmmunition.class);
            var types = mockStatic(FlansEntityTypes.class);
            var projectiles = mockStatic(FlansProjectiles.class);
            var ignored = mockConstruction(NPCAttackSelector.class, (selector, context) -> when(selector.isEntityApplicable(any())).thenReturn(true)))
        {
            models.when(() -> NpcModels.flan(vehicle)).thenReturn(model);
            models.when(() -> NpcModels.world(eq(vehicle), eq(model), any(), anyFloat())).thenReturn(Vec3.ZERO);
            states.when(() -> NpcVehicleState.of(vehicle)).thenReturn(state);
            states.when(() -> NpcVehicleState.aimPoint(aside)).thenReturn(targetPoint);
            states.when(() -> NpcVehicleState.aimPoint(ahead)).thenReturn(otherPoint);
            crew.when(() -> NpcCrew.occupant(vehicle, 1)).thenReturn(Optional.of(gunner));
            ammunition.when(() -> NpcAmmunition.crewGunTemplate(vehicle, gunner, weapon)).thenReturn(ammo);
            types.when(() -> FlansEntityTypes.getSeatReloadCycle(definition, 1, ammo)).thenReturn(Optional.of(new ReloadCycle(2, 2D, 4D, OptionalInt.empty())));
            projectiles.when(() -> FlansProjectiles.getSeatMuzzle(eq(definition), eq(1), anyInt(), eq(MountAim.REST), any()))
                .thenReturn(Optional.of(new WeaponMuzzle(Vec3.ZERO, Optional.of(weapon), List.of())));
            CrewGunners guns = new CrewGunners();
            float[] yaws = hullYaws.length == 0 ? new float[]{0F} : hullYaws;
            for (int tick = 0; tick < yaws.length; tick++)
            {
                when(state.getHullYaw()).thenReturn(yaws[tick]);
                vehicle.tickCount = tick;
                guns.tick(vehicle);
            }
        }
        return leads;
    }
}
