package com.flansmodultimate.common.guns;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmplacementControlTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void virtualMagazinesStayWithTheirOwnerAndNeverBecomeFiniteLoot()
    {
        Entity owner = mock(Entity.class);
        when(owner.getUUID()).thenReturn(UUID.randomUUID());
        when(owner.isAlive()).thenReturn(true);
        Entity other = mock(Entity.class);
        when(other.getUUID()).thenReturn(UUID.randomUUID());
        when(other.isAlive()).thenReturn(true);
        EmplacementControl control = new EmplacementControl();
        ItemStack magazine = new ItemStack(Items.IRON_NUGGET);
        control.supplied(magazine, owner, true);
        assertTrue(control.isVirtual(magazine));
        assertSame(magazine, control.retain(magazine, owner));
        assertTrue(control.retain(magazine, other).isEmpty());
        control.supplied(magazine, owner, true);
        assertTrue(control.retain(magazine, null).isEmpty());
        control.supplied(magazine, owner, true);
        assertTrue(control.finite(magazine).isEmpty());
        ItemStack finite = new ItemStack(Items.GOLD_NUGGET);
        control.supplied(finite, owner, false);
        assertSame(finite, control.retain(finite, null));
        assertSame(finite, control.finite(finite));
    }

    @Test
    void automatedAimExpiresAndBelongsOnlyToItsLivingOperator()
    {
        EmplacementControl control = new EmplacementControl();
        Entity owner = mock(Entity.class);
        when(owner.getUUID()).thenReturn(UUID.randomUUID());
        when(owner.isAlive()).thenReturn(true);
        Entity other = mock(Entity.class);
        when(other.getUUID()).thenReturn(UUID.randomUUID());
        when(other.isAlive()).thenReturn(true);
        Vec3 direction = new Vec3(1D, 1D, 0D);
        control.aim(100L, owner, direction);
        assertEquals(direction, control.aim(100L, owner).orElseThrow());
        assertEquals(direction, control.aim(101L, owner).orElseThrow());
        assertTrue(control.aim(100L, other).isEmpty());
        assertTrue(control.aim(102L, owner).isEmpty());
        when(owner.isAlive()).thenReturn(false);
        assertTrue(control.aim(100L, owner).isEmpty());
        when(owner.isAlive()).thenReturn(true);
        control.clearAim();
        assertTrue(control.aim(100L, owner).isEmpty());
    }

    @Test
    void stoppedOrFrozenControllersCannotLeaveTheTriggerHeld()
    {
        EmplacementControl control = new EmplacementControl();
        Entity owner = mock(Entity.class);
        when(owner.getUUID()).thenReturn(UUID.randomUUID());
        when(owner.isAlive()).thenReturn(true);
        Entity other = mock(Entity.class);
        when(other.getUUID()).thenReturn(UUID.randomUUID());
        when(other.isAlive()).thenReturn(true);
        assertFalse(control.firing(100L, owner));
        control.firing(100L, owner, true);
        assertTrue(control.firing(100L, owner));
        assertTrue(control.firing(101L, owner));
        assertFalse(control.firing(100L, other));
        assertFalse(control.firing(100L, null));
        assertFalse(control.firing(102L, owner));
        control.firing(102L, owner, false);
        assertFalse(control.firing(102L, owner));
    }
}
