package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcDriverGunsTest
{
    private static final class Battle implements AutoCloseable
    {
        private final MockedStatic<FlansProjectiles> projectiles = mockStatic(FlansProjectiles.class);
        private final MockedStatic<FlansEquipment> equipment = mockStatic(FlansEquipment.class);
        private final MockedStatic<NpcAmmunition> ammunition = mockStatic(NpcAmmunition.class);
        private final MockedStatic<NpcModels> models = mockStatic(NpcModels.class);
        private final MockedStatic<NpcVehicleState> states = mockStatic(NpcVehicleState.class);
        private final MockedStatic<NpcSight> sight = mockStatic(NpcSight.class);
        private final MockedStatic<NpcDrivers> drivers = mockStatic(NpcDrivers.class);
        private final MockedStatic<NpcCrew> crew = mockStatic(NpcCrew.class);
        private final EntityNPCInterface npc = mock(EntityNPCInterface.class);
        private final NpcVehicleState state = mock(NpcVehicleState.class, RETURNS_DEEP_STUBS);
        private final NpcWeaponOptions options = new NpcWeaponOptions();
        private final NpcDriverGuns guns = new NpcDriverGuns();
        private final IDriveableType definition = mock(IDriveableType.class);
        private final IContentType weapon = mock(IContentType.class);
        private final List<Integer> fired = new ArrayList<>();
        private int tick;

        private Battle()
        {
            ProjectileShot shot = mock(ProjectileShot.class);
            options.set(Feature.DRIVER_GUNS, true);
            options.set(Feature.ALTERNATE_BARRELS, false);
            ServerLevel level = mock(ServerLevel.class);
            MinecraftServer server = mock(MinecraftServer.class);
            when(level.getServer()).thenReturn(server);
            when(server.isSameThread()).thenReturn(true);
            when(npc.level()).thenReturn(level);
            when(npc.isAlive()).thenReturn(true);
            LivingEntity target = mock(LivingEntity.class);
            when(target.level()).thenReturn(level);
            when(target.isAlive()).thenReturn(true);
            when(npc.getTarget()).thenReturn(target);
            npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
            npc.stats.ranged = mock(DataRanged.class);
            when(npc.stats.ranged.getRange()).thenReturn(100);
            when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
            when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning()).thenReturn(new NpcTuning());
            FlanModelEntity model = mock(FlanModelEntity.class);
            when(model.getInfoType()).thenReturn(definition);
            models.when(() -> NpcModels.flan(npc)).thenReturn(model);
            models.when(() -> NpcModels.world(eq(npc), eq(model), any(), anyFloat())).thenReturn(Vec3.ZERO);
            states.when(() -> NpcVehicleState.of(npc)).thenReturn(state);
            when(state.isAligned()).thenReturn(true);
            when(state.turretAim()).thenReturn(MountAim.REST);
            when(state.lead(any(), eq(target), eq(options), anyFloat(), anyDouble(), anyDouble(), eq(false))).thenReturn(new Vec3(0D, 0D, -1D));
            states.when(() -> NpcVehicleState.aimPoint(target)).thenReturn(new Vec3(0D, 0D, -10D));
            sight.when(() -> NpcSight.clear(eq(level), any(), any(), anyInt(), eq(npc))).thenReturn(true);
            when(weapon.getItem()).thenReturn(Items.STICK);
            ItemStack ammo = new ItemStack(Items.IRON_NUGGET);
            ammunition.when(() -> NpcAmmunition.driverGunTemplate(npc, weapon)).thenReturn(ammo);
            EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
            when(properties.shotDelay()).thenReturn(2D);
            when(properties.reloadTime()).thenReturn(3D);
            equipment.when(() -> FlansEquipment.getWeaponProperties(any(), eq(ammo))).thenReturn(Optional.of(properties));
            WeaponMuzzle muzzle = new WeaponMuzzle(Vec3.ZERO, Optional.of(weapon), List.of());
            projectiles.when(() -> FlansProjectiles.getMuzzles(definition, true, 0, false, MountAim.REST)).thenReturn(List.of(muzzle));
            projectiles.when(() -> FlansProjectiles.getRoundsPerItem(ammo)).thenReturn(2);
            projectiles.when(() -> FlansProjectiles.prepare(eq(npc), eq(ammo), eq(ItemStack.EMPTY), eq(weapon), eq(definition), eq(true), any())).thenReturn(Optional.of(shot));
            when(shot.speed()).thenReturn(1F);
            when(shot.launch(any(), any(), eq(false))).thenAnswer(call ->
            {
                fired.add(tick);
                return List.of(mock(Entity.class));
            });
        }

        private void advance(int ticks)
        {
            for (int i = 0; i < ticks; i++)
            {
                guns.tick(npc);
                tick += 1;
            }
        }

        @Override
        public void close()
        {
            crew.close();
            drivers.close();
            sight.close();
            states.close();
            models.close();
            ammunition.close();
            equipment.close();
            projectiles.close();
        }
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void extraBankFiresAtGunCadenceAndReloadsWithoutTriggeringTheMainBank()
    {
        try (Battle battle = new Battle())
        {
            battle.advance(8);
            assertEquals(List.of(0, 2, 5, 7), battle.fired);
            battle.projectiles.verify(() -> FlansProjectiles.getMuzzles(eq(battle.definition), eq(false), anyInt(), anyBoolean(), any()), never());
            battle.ammunition.verify(() -> NpcAmmunition.driverGunTemplate(battle.npc, battle.weapon), times(4));
        }
    }

    @Test
    void authoredAlternationSelectsOneBarrelPerTrigger()
    {
        try (Battle battle = new Battle())
        {
            battle.options.set(Feature.ALTERNATE_BARRELS, true);
            WeaponMuzzle first = new WeaponMuzzle(Vec3.ZERO, Optional.of(battle.weapon), List.of());
            WeaponMuzzle second = new WeaponMuzzle(new Vec3(1D, 0D, 0D), Optional.of(battle.weapon), List.of());
            battle.projectiles.when(() -> FlansProjectiles.getMuzzles(battle.definition, true, 0, false, MountAim.REST)).thenReturn(List.of(first, second));
            battle.projectiles.when(() -> FlansProjectiles.getMuzzles(battle.definition, true, 0, false)).thenReturn(List.of(first, second));
            battle.projectiles.when(() -> FlansProjectiles.getMuzzles(eq(battle.definition), eq(true), anyInt(), eq(true), eq(MountAim.REST)))
                .thenAnswer(call -> List.of(call.<Integer>getArgument(2) % 2 == 0 ? first : second));
            battle.advance(3);
            assertEquals(List.of(0, 2), battle.fired);
            battle.projectiles.verify(() -> FlansProjectiles.getMuzzles(battle.definition, true, 0, true, MountAim.REST));
            battle.projectiles.verify(() -> FlansProjectiles.getMuzzles(battle.definition, true, 1, true, MountAim.REST));
        }
    }

    @Test
    void aDriverSeatGunAlsoFiresWhenItBelongsToNeitherBank()
    {
        try (Battle battle = new Battle())
        {
            battle.options.set(Feature.ALTERNATE_BARRELS, true);
            battle.projectiles.when(() -> FlansProjectiles.getMuzzles(battle.definition, true, 0, false, MountAim.REST)).thenReturn(List.of());
            WeaponMuzzle driver = new WeaponMuzzle(Vec3.ZERO, Optional.of(battle.weapon), List.of());
            battle.projectiles.when(() -> FlansProjectiles.getSeatMuzzle(eq(battle.definition), eq(0), anyInt(), eq(MountAim.REST))).thenReturn(Optional.of(driver));
            battle.advance(8);
            assertEquals(List.of(0, 2, 5, 7), battle.fired);
        }
    }

    @Test
    void disabledGunsAndDamagedOrUnalignedVehiclesHoldFire()
    {
        try (Battle battle = new Battle())
        {
            battle.options.set(Feature.DRIVER_GUNS, false);
            battle.advance(3);
            battle.options.set(Feature.DRIVER_GUNS, true);
            when(battle.state.isAligned()).thenReturn(false);
            battle.advance(3);
            when(battle.state.isAligned()).thenReturn(true);
            when(battle.state.getParts().effects().gunDisabled()).thenReturn(true);
            battle.advance(3);
            when(battle.state.getParts().effects().gunDisabled()).thenReturn(false);
            battle.advance(1);
            assertEquals(List.of(9), battle.fired);
        }
    }
}
