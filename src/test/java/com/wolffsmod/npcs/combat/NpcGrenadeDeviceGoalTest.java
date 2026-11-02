package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.projectiles.DetonationRadii;
import com.flansmodultimate.api.projectiles.FlansGrenadeDevices;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.GrenadeDeviceProperties;
import com.flansmodultimate.api.projectiles.IProximityTarget;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.tactics.NpcCombatMovement;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataInventory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcGrenadeDeviceGoalTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static EntityNPCInterface npc(ServerLevel level) throws ReflectiveOperationException
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        var ai = EntityNPCInterface.class.getDeclaredField("ais");
        ai.setAccessible(true);
        ai.set(npc, new DataAI(npc));
        npc.inventory = new DataInventory(npc);
        when(npc.level()).thenReturn(level);
        when(npc.isAlive()).thenReturn(true);
        when(npc.position()).thenReturn(Vec3.ZERO);
        when(npc.getBoundingBox()).thenReturn(new AABB(0D, 0D, 0D, 1D, 2D, 1D));
        when(npc.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        when(npc.getUUID()).thenReturn(UUID.randomUUID());
        return npc;
    }

    private static void enabled(MockedStatic<NpcEquipment> equipment, MockedStatic<TacticKind> kinds, EntityNPCInterface npc)
    {
        equipment.when(() -> NpcEquipment.enabled(eq(npc), any())).thenReturn(true);
        kinds.when(() -> TacticKind.of(npc)).thenReturn(TacticKind.STANDARD);
    }

    @Test
    void mineTypesAreMatchedToInfantryOrMachineryRatherThanTheLivingEntityBaseClass()
    {
        LivingEntity infantry = mock(LivingEntity.class);
        LivingEntity vehicle = mock(LivingEntity.class, withSettings().extraInterfaces(IProximityTarget.class));
        when(((IProximityTarget) vehicle).flansmodultimateDriveableProximityTarget()).thenReturn(true);
        GrenadeDeviceProperties antiPersonnel = new GrenadeDeviceProperties(2D, 0D, 40, false, false);
        GrenadeDeviceProperties antiTank = new GrenadeDeviceProperties(0D, 3D, 40, false, false);
        assertTrue(NpcGrenadeDeviceGoal.targetForMine(antiPersonnel, infantry));
        assertFalse(NpcGrenadeDeviceGoal.targetForMine(antiPersonnel, vehicle));
        assertTrue(NpcGrenadeDeviceGoal.targetForMine(antiTank, vehicle));
        assertFalse(NpcGrenadeDeviceGoal.targetForMine(antiTank, infantry));
        LivingEntity horse = mock(LivingEntity.class);
        when(infantry.getVehicle()).thenReturn(horse);
        assertTrue(NpcGrenadeDeviceGoal.targetForMine(antiPersonnel, infantry));
        assertFalse(NpcGrenadeDeviceGoal.targetForMine(antiTank, infantry));
        Entity driveable = mock(Entity.class);
        Entity seat = mock(Entity.class, withSettings().extraInterfaces(ISeat.class));
        when(((ISeat) seat).getDriveable()).thenReturn(driveable);
        when(infantry.getVehicle()).thenReturn(seat);
        assertTrue(NpcGrenadeDeviceGoal.targetForMine(antiTank, infantry));
        assertFalse(NpcGrenadeDeviceGoal.targetForMine(antiPersonnel, infantry));
    }

    @Test
    void remoteChargesWaitForClearanceFromTheirOperatorAndAllies() throws ReflectiveOperationException
    {
        ServerLevel level = mock(ServerLevel.class);
        EntityNPCInterface npc = npc(level);
        Entity charge = mock(Entity.class);
        when(charge.getBoundingBox()).thenReturn(new AABB(7D, 0D, 0D, 8D, 1D, 1D));
        when(charge.position()).thenReturn(new Vec3(8D, 0D, 0D));
        when(npc.distanceTo(charge)).thenReturn(8F);
        LivingEntity enemy = mock(LivingEntity.class);
        when(enemy.isAlive()).thenReturn(true);
        when(enemy.distanceToSqr(charge)).thenReturn(1D);
        when(npc.hasLineOfSight(enemy)).thenReturn(true);
        when(level.getEntitiesOfClass(eq(LivingEntity.class), any(), any())).thenAnswer(call ->
        {
            Predicate<LivingEntity> wanted = call.getArgument(2);
            return wanted.test(enemy) ? List.of(enemy) : List.of();
        });
        try (var devices = mockStatic(FlansGrenadeDevices.class);
            var equipment = mockStatic(NpcEquipment.class);
            var kinds = mockStatic(TacticKind.class);
            var drivers = mockStatic(NpcDrivers.class);
            var crew = mockStatic(NpcCrew.class);
            var platform = mockStatic(CustomNpcsPlatform.class);
            var supplies = mockStatic(NpcDeviceSupplies.class))
        {
            enabled(equipment, kinds, npc);
            devices.when(() -> FlansGrenadeDevices.find(eq(level), any())).thenReturn(List.of(charge));
            devices.when(() -> FlansGrenadeDevices.properties(charge)).thenReturn(Optional.of(new GrenadeDeviceProperties(0D, 0D, 0, true, false)));
            devices.when(() -> FlansGrenadeDevices.canDetonate(charge, npc)).thenReturn(true);
            devices.when(() -> FlansGrenadeDevices.detonate(charge, npc)).thenReturn(true);
            devices.when(() -> FlansGrenadeDevices.radii(charge)).thenReturn(Optional.of(new DetonationRadii(0D, 4D, 0D, 0D)));
            drivers.when(() -> NpcDrivers.hostile(npc, enemy)).thenReturn(true);
            NpcGrenadeDeviceGoal goal = new NpcGrenadeDeviceGoal(npc);
            assertFalse(goal.canUse());
            devices.verify(() -> FlansGrenadeDevices.detonate(charge, npc));
            when(level.getGameTime()).thenReturn(20L);
            when(npc.distanceTo(charge)).thenReturn(3F);
            goal.canUse();
            devices.verify(() -> FlansGrenadeDevices.detonate(charge, npc), times(1));
            when(level.getGameTime()).thenReturn(40L);
            when(npc.distanceTo(charge)).thenReturn(8F);
            LivingEntity ally = mock(LivingEntity.class);
            when(level.getEntities(eq(npc), any(AABB.class), any())).thenReturn(List.of(ally));
            goal.canUse();
            devices.verify(() -> FlansGrenadeDevices.detonate(charge, npc), times(1));
        }
    }

    @Test
    void failedPlacementPreservesTheCarriedMine() throws ReflectiveOperationException
    {
        ServerLevel level = mock(ServerLevel.class);
        EntityNPCInterface npc = npc(level);
        LivingEntity target = mock(LivingEntity.class);
        when(npc.getTarget()).thenReturn(target);
        when(npc.hasLineOfSight(target)).thenReturn(true);
        when(npc.distanceTo(target)).thenReturn(20F);
        when(target.position()).thenReturn(new Vec3(20D, 0D, 0D));
        when(target.isAlive()).thenReturn(true);
        when(target.onGround()).thenReturn(true);
        PathNavigation navigation = mock(PathNavigation.class);
        Path path = mock(Path.class);
        when(npc.getNavigation()).thenReturn(navigation);
        when(navigation.createPath(any(BlockPos.class), anyInt())).thenReturn(path);
        when(path.canReach()).thenReturn(true);
        when(level.clip(any())).thenReturn(new BlockHitResult(new Vec3(3.5D, 0D, 0.5D), Direction.UP, new BlockPos(3, -1, 0), false));
        ItemStack mine = new ItemStack(Items.STICK);
        IItemStack wrapped = mock(IItemStack.class);
        when(wrapped.getMCItemStack()).thenReturn(mine);
        npc.inventory.drops.put(0, wrapped);
        try (var devices = mockStatic(FlansGrenadeDevices.class);
            var equipment = mockStatic(NpcEquipment.class);
            var kinds = mockStatic(TacticKind.class);
            var drivers = mockStatic(NpcDrivers.class);
            var crew = mockStatic(NpcCrew.class);
            var platform = mockStatic(CustomNpcsPlatform.class);
            var movement = mockStatic(NpcCombatMovement.class);
            var projectiles = mockStatic(FlansProjectiles.class))
        {
            enabled(equipment, kinds, npc);
            devices.when(() -> FlansGrenadeDevices.properties(mine)).thenReturn(Optional.of(new GrenadeDeviceProperties(2D, 0D, 40, false, false)));
            projectiles.when(() -> FlansProjectiles.isProjectile(mine)).thenReturn(true);
            projectiles.when(() -> FlansProjectiles.getDetonationRadii(mine)).thenReturn(Optional.of(new DetonationRadii(0D, 4D, 0D, 0D)));
            movement.when(() -> NpcCombatMovement.permits(eq(npc), any())).thenReturn(true);
            NpcGrenadeDeviceGoal goal = new NpcGrenadeDeviceGoal(npc);
            assertTrue(goal.canUse());
            goal.tick();
            devices.verify(() -> FlansGrenadeDevices.place(eq(npc), eq(mine), eq(new BlockPos(3, -1, 0)), any(), eq(true)));
            assertEquals(1, mine.getCount());
            assertSame(wrapped, npc.inventory.drops.get(0));
            assertFalse(goal.canContinueToUse());
        }
    }
}
