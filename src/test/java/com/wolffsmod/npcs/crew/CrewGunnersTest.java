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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
            projectiles.when(() -> FlansProjectiles.getSeatMuzzle(eq(definition), eq(1), anyInt(), eq(MountAim.REST))).thenReturn(Optional.of(muzzle));
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
}
