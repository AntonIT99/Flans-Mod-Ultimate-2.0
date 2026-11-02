package com.flansmodultimate.common.entity;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriveableAccessQueryTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void availabilityQueriesNeverBindKeysOrTakeOwnership()
    {
        Driveable vehicle = mock(Vehicle.class);
        Player player = mock(Player.class);
        Abilities abilities = new Abilities();
        UUID playerId = UUID.randomUUID();
        when(player.getUUID()).thenReturn(playerId);
        when(player.getAbilities()).thenReturn(abilities);
        when(player.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        when(player.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        doCallRealMethod().when(vehicle).canPlayerAccessWithoutBinding(player);
        vehicle.locked = true;
        assertFalse(vehicle.canPlayerAccessWithoutBinding(player));
        assertNull(vehicle.ownerId);
        verify(vehicle, never()).bindOrCheckKey(any(), any());
        vehicle.ownerId = playerId;
        assertTrue(vehicle.canPlayerAccessWithoutBinding(player));
        vehicle.ownerId = UUID.randomUUID();
        assertFalse(vehicle.canPlayerAccessWithoutBinding(player));
        abilities.instabuild = true;
        assertTrue(vehicle.canPlayerAccessWithoutBinding(player));
    }
}
