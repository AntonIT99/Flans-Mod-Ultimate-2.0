package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.driveables.IFlanSeatOccupant;
import com.flansmodultimate.common.FlanEntityPermissions;
import com.flansmodultimate.platform.PlatformEvents;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeatOccupantsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void availabilityDoesNotDisplaceAnyoneAndDeploymentCanBeDisabledIndependently()
    {
        Entity mount = mock(Entity.class);
        ServerLevel level = mock(ServerLevel.class);
        when(mount.level()).thenReturn(level);
        ServerPlayer player = mock(ServerPlayer.class);
        Entity operator = mock(Entity.class, withSettings().extraInterfaces(IFlanSeatOccupant.class));
        IFlanSeatOccupant policy = (IFlanSeatOccupant) operator;
        AtomicReference<List<Entity>> riders = new AtomicReference<>(List.of(operator));
        when(mount.getPassengers()).thenAnswer(call -> riders.get());
        when(policy.flansmodultimateMayYieldSeat(player, false)).thenReturn(true);
        assertTrue(SeatOccupants.canEnter(mount, player, false));
        assertFalse(SeatOccupants.canEnter(mount, player, true));
        verify(policy, never()).flansmodultimateYieldSeat(any(), anyBoolean());
        assertFalse(SeatOccupants.makeRoom(mount, player, true));
        when(policy.flansmodultimateYieldSeat(player, false)).thenAnswer(call ->
        {
            riders.set(List.of());
            return true;
        });
        assertTrue(SeatOccupants.makeRoom(mount, player, false));
        assertTrue(riders.get().isEmpty());
        Player otherPlayer = mock(Player.class);
        riders.set(List.of(otherPlayer));
        assertFalse(SeatOccupants.canEnter(mount, player, false));
        assertFalse(SeatOccupants.makeRoom(mount, player, true));
        verify(otherPlayer, never()).stopRiding();
    }

    @Test
    void rejectedEntryEventsAndPermissionsLeaveTheNpcInItsSeat()
    {
        Seat seat = mock(Seat.class);
        ServerLevel level = mock(ServerLevel.class);
        when(seat.level()).thenReturn(level);
        Driveable vehicle = mock(Driveable.class);
        seat.driveable = vehicle;
        ServerPlayer player = mock(ServerPlayer.class);
        Entity operator = mock(Entity.class, withSettings().extraInterfaces(IFlanSeatOccupant.class));
        IFlanSeatOccupant policy = (IFlanSeatOccupant) operator;
        when(seat.getPassengers()).thenReturn(List.of(operator));
        when(policy.flansmodultimateMayYieldSeat(player, false)).thenReturn(true);
        when(vehicle.canPlayerAccess(player)).thenReturn(true);
        when(seat.tryEnter(player, false)).thenCallRealMethod();
        try (var permissions = mockStatic(PermissionAPI.class); var events = mockStatic(PlatformEvents.class))
        {
            permissions.when(() -> PermissionAPI.getPermission(player, FlanEntityPermissions.DRIVEABLE_ENTER)).thenReturn(false);
            assertFalse(seat.tryEnter(player, false));
            permissions.when(() -> PermissionAPI.getPermission(player, FlanEntityPermissions.DRIVEABLE_ENTER)).thenReturn(true);
            events.when(() -> PlatformEvents.postCancellable(any())).thenReturn(true);
            assertFalse(seat.tryEnter(player, false));
            verify(policy, never()).flansmodultimateYieldSeat(any(), anyBoolean());
            verify(player, never()).startRiding(any(), anyBoolean());
        }
    }
}
