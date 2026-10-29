package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.FlanEntityPermissions;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriveablePickupTest
{
    private Driveable driveable;
    private Player player;
    private DamageSource source;
    private Abilities abilities;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @BeforeEach
    void setup()
    {
        driveable = mock(Vehicle.class);
        player = mock(Player.class);
        source = mock(DamageSource.class);
        abilities = new Abilities();
        abilities.instabuild = true;
        when(player.getAbilities()).thenReturn(abilities);
        when(source.getEntity()).thenReturn(player);
        when(driveable.canPlayerAccess(player)).thenReturn(true);
        ServerLevel level = mock(ServerLevel.class);
        when(driveable.level()).thenReturn(level);
        doCallRealMethod().when(driveable).tryPickupOnAttack(source);
        doCallRealMethod().when(driveable).hurt(source, 1F);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hull", "seat", "wheel"})
    void creativeHitPicksUpAnUnsupportedDriveableThroughHullOrProxies(String target)
    {
        Entity hit = driveable;
        if (target.equals("seat"))
        {
            Seat seat = mock(Seat.class);
            seat.driveable = driveable;
            doCallRealMethod().when(seat).hurt(source, 1F);
            hit = seat;
        }
        else if (target.equals("wheel"))
        {
            Wheel wheel = mock(Wheel.class);
            wheel.driveable = driveable;
            doCallRealMethod().when(wheel).hurt(source, 1F);
            hit = wheel;
        }

        assertTrue(hit.hurt(source, 1F));
        verify(driveable).pickupAsItem(player);
        verify(driveable, never()).damagePart(any(), anyFloat(), any());
    }

    @Test
    void survivalStillRequiresGroundSupport()
    {
        abilities.instabuild = false;
        assertFalse(driveable.tryPickupOnAttack(source));
        verify(driveable, never()).pickupAsItem(any());
    }

    @Test
    void occupiedDriveablesCannotBePickedUpInCreative()
    {
        when(driveable.hasDriveableOccupant()).thenReturn(true);
        assertFalse(driveable.tryPickupOnAttack(source));
        verify(driveable, never()).pickupAsItem(any());
    }

    @Test
    void creativePickupStillRequiresPermission()
    {
        ServerPlayer deniedPlayer = mock(ServerPlayer.class);
        when(deniedPlayer.getAbilities()).thenReturn(abilities);
        when(source.getEntity()).thenReturn(deniedPlayer);
        when(driveable.canPlayerAccess(deniedPlayer)).thenReturn(true);
        try (var permissions = mockStatic(PermissionAPI.class))
        {
            permissions.when(() -> PermissionAPI.getPermission(deniedPlayer, FlanEntityPermissions.DRIVEABLE_PICKUP)).thenReturn(false);
            assertTrue(driveable.tryPickupOnAttack(source));
            verify(driveable, never()).pickupAsItem(any());
        }
    }
}
