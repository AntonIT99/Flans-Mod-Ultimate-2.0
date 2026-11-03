package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.driveables.WeaponTriggerState.Mode;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.MechaItemType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MechaDriverToolsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void swordsAndDrillsExposeOriginsAndReachWithoutBeingGuns()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Mecha mecha = spy(new Mecha(EntityType.ARMOR_STAND, world.level));
            MechaType type = mock(MechaType.class);
            when(type.getReach()).thenReturn(2F);
            doReturn(type).when(mecha).getMechaType();
            doReturn(true).when(mecha).isPartIntact(any());
            mecha.driveableData = mock(DriveableData.class);
            ItemStack sword = tool(EnumMechaToolType.SWORD);
            ItemStack drill = tool(EnumMechaToolType.PICKAXE);
            when(mecha.driveableData.getMechaAddon(EnumMechaSlotType.LEFT_TOOL)).thenReturn(sword);
            when(mecha.driveableData.getMechaAddon(EnumMechaSlotType.RIGHT_TOOL)).thenReturn(drill);
            assertNotNull(mecha.getDriverWeaponOrigin(false));
            assertNotNull(mecha.getDriverWeaponDirection(false));
            assertEquals(6D, mecha.getDriverMeleeProperties(false).orElseThrow().reach());
            assertTrue(mecha.getDriverMiningReach(false).isEmpty());
            assertEquals(6D, mecha.getDriverMiningReach(true).orElseThrow());
            assertTrue(mecha.getDriverMeleeProperties(true).isEmpty());
            doReturn(false).when(mecha).isPartIntact(EnumDriveablePart.RIGHT_ARM);
            assertNull(mecha.getDriverWeaponOrigin(true));
            assertTrue(mecha.getDriverMiningReach(true).isEmpty());
        }
    }

    @Test
    void selectedDriverBankUsesItsOwnOriginForAim()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Vehicle vehicle = spy(new Vehicle(EntityType.ARMOR_STAND, world.level));
            Seat seat = spy(new Seat(EntityType.ARMOR_STAND, world.level));
            seat.driveable = vehicle;
            LivingEntity occupant = mock(LivingEntity.class);
            when(occupant.isAlive()).thenReturn(true);
            doReturn(occupant).when(seat).getFirstPassenger();
            doReturn(true).when(vehicle).isMovementController(seat);
            doReturn(true).when(seat).setAutomatedAim(anyFloat(), anyFloat());
            Vec3 toolOrigin = Vec3.ZERO;
            Vec3 gunOrigin = new Vec3(4D, 0D, 0D);
            doReturn(toolOrigin).when(vehicle).getDriverWeaponOrigin(false);
            doReturn(gunOrigin).when(vehicle).getDriverWeaponOrigin(true);
            doAnswer(call -> Vec3.directionFromRotation(call.getArgument(1), call.getArgument(0))).when(vehicle).aimedDirection(anyFloat(), anyFloat());
            Vec3 target = new Vec3(0D, 0D, 10D);
            assertTrue(seat.aimDriverWeapon(false, target));
            assertTrue(seat.aimDriverWeapon(true, target));
            ArgumentCaptor<Float> yaw = ArgumentCaptor.forClass(Float.class);
            verify(seat, times(2)).setAutomatedAim(yaw.capture(), anyFloat());
            assertNotEquals(yaw.getAllValues().get(0), yaw.getAllValues().get(1));
            verify(vehicle).getDriverWeaponOrigin(false);
            verify(vehicle).getDriverWeaponOrigin(true);
        }
    }

    @Test
    void vehicleBanksReportTheirOwnBurstState()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Vehicle vehicle = spy(new Vehicle(EntityType.ARMOR_STAND, world.level));
            doReturn(EnumFireMode.SEMIAUTO).when(vehicle).getWeaponBankFireMode(false);
            doReturn(EnumFireMode.BURST).when(vehicle).getWeaponBankFireMode(true);
            vehicle.secondaryBurstRemaining = 2;
            assertEquals(Mode.SEMIAUTOMATIC, vehicle.getDriverTriggerState(false).mode());
            assertTrue(vehicle.getDriverTriggerState(false).needsRelease());
            assertEquals(Mode.BURST, vehicle.getDriverTriggerState(true).mode());
            assertTrue(vehicle.getDriverTriggerState(true).burstActive());
            assertFalse(vehicle.getDriverTriggerState(true).needsRelease());
            vehicle.secondaryBurstRemaining = 0;
            assertTrue(vehicle.getDriverTriggerState(true).needsRelease());
        }
    }

    @Test
    void handsReportTheirOwnFireModes()
    {
        try (EntityTestWorld world = new EntityTestWorld())
        {
            Mecha mecha = new Mecha(EntityType.ARMOR_STAND, world.level);
            mecha.driveableData = mock(DriveableData.class);
            ItemStack left = gun(EnumFireMode.SEMIAUTO);
            ItemStack right = gun(EnumFireMode.BURST);
            when(mecha.driveableData.getMechaAddon(EnumMechaSlotType.LEFT_TOOL)).thenReturn(left);
            when(mecha.driveableData.getMechaAddon(EnumMechaSlotType.RIGHT_TOOL)).thenReturn(right);
            assertEquals(Mode.SEMIAUTOMATIC, mecha.getDriverTriggerState(false).mode());
            assertEquals(Mode.BURST, mecha.getDriverTriggerState(true).mode());
            assertTrue(mecha.getDriverTriggerState(true).needsRelease());
        }
    }

    private static ItemStack tool(EnumMechaToolType function)
    {
        MechaItemType type = mock(MechaItemType.class);
        when(type.getMechaItemType()).thenReturn(EnumMechaItemType.TOOL);
        when(type.getFunction()).thenReturn(function);
        when(type.getSpeed()).thenReturn(1F);
        when(type.getReach()).thenReturn(3F);
        MechaAddonItem item = mock(MechaAddonItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        return stack;
    }

    private static ItemStack gun(EnumFireMode mode)
    {
        GunType type = mock(GunType.class);
        GunItem item = mock(GunItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        when(type.getFireMode(stack)).thenReturn(mode);
        return stack;
    }
}
