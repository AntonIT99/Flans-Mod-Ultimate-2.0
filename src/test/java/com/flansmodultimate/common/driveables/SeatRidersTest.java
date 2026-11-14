package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SeatRidersTest
{
    private static final long LOADED_AT = 1000L;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void onlyRidersOfSeatsOtherThanPlayersSaveAlone()
    {
        Seat seat = mock(Seat.class);
        LivingEntity gunner = mock(LivingEntity.class);
        when(gunner.getVehicle()).thenReturn(seat);
        Player player = mock(Player.class);
        when(player.getVehicle()).thenReturn(seat);
        LivingEntity horseman = mock(LivingEntity.class);
        when(horseman.getVehicle()).thenReturn(mock(Entity.class));

        assertTrue(SeatRiders.savesAlone(gunner));
        assertFalse(SeatRiders.savesAlone(player));
        assertFalse(SeatRiders.savesAlone(horseman));
        assertFalse(SeatRiders.savesAlone(mock(LivingEntity.class)));
    }

    @Test
    void aRiderIsSavedWithTheSeatItSatIn()
    {
        UUID driveableId = UUID.randomUUID();
        Driveable driveable = mock(Driveable.class);
        when(driveable.getUUID()).thenReturn(driveableId);
        Seat seat = mock(Seat.class);
        when(seat.getDriveable()).thenReturn(driveable);
        when(seat.getSeatIndex()).thenReturn(2);
        LivingEntity gunner = mock(LivingEntity.class);
        when(gunner.getVehicle()).thenReturn(seat);
        CompoundTag tag = new CompoundTag();

        // An entity vanilla refuses to save, one being discarded say, stays unsaved.
        assertFalse(SeatRiders.save(gunner, tag));
        assertNull(SeatRiders.SavedSeat.load(tag, LOADED_AT));

        when(gunner.saveAsPassenger(tag)).thenReturn(true);
        assertTrue(SeatRiders.save(gunner, tag));
        assertEquals(new SeatRiders.SavedSeat(driveableId, 2, LOADED_AT), SeatRiders.SavedSeat.load(tag, LOADED_AT));
    }

    @Test
    void aSavedSeatAnswersItsOwnDriveableOnlyShortlyAfterLoading()
    {
        UUID driveableId = UUID.randomUUID();
        SeatRiders.SavedSeat saved = new SeatRiders.SavedSeat(driveableId, 0, LOADED_AT);

        assertTrue(saved.answers(driveableId, LOADED_AT));
        assertTrue(saved.answers(driveableId, LOADED_AT + SeatRiders.RECALL_TICKS));
        // A rider that has long gone its own way is not pulled back when its driveable loads again.
        assertFalse(saved.answers(driveableId, LOADED_AT + SeatRiders.RECALL_TICKS + 1));
        assertFalse(saved.answers(UUID.randomUUID(), LOADED_AT));
    }

    @Test
    void aLoadedDriveableCallsItsRidersBackToTheirSeats()
    {
        UUID driveableId = UUID.randomUUID();
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameTime()).thenReturn(LOADED_AT + 20L);
        Driveable driveable = mock(Driveable.class);
        when(driveable.getUUID()).thenReturn(driveableId);
        when(driveable.level()).thenReturn(level);
        Seat driverSeat = seat();
        Seat gunnerSeat = seat();
        Entity gunner = rider(new SeatRiders.SavedSeat(driveableId, 1, LOADED_AT));
        Entity stranger = rider(new SeatRiders.SavedSeat(UUID.randomUUID(), 0, LOADED_AT));
        Entity latecomer = rider(new SeatRiders.SavedSeat(driveableId, 0, LOADED_AT - 10L * SeatRiders.RECALL_TICKS));
        Entity mounted = rider(new SeatRiders.SavedSeat(driveableId, 0, LOADED_AT));
        when(mounted.isPassenger()).thenReturn(true);
        Entity misplaced = rider(new SeatRiders.SavedSeat(driveableId, 7, LOADED_AT));
        List<Entity> nearby = List.of(gunner, stranger, latecomer, mounted, misplaced, mock(Entity.class));
        when(level.getEntities(eq(driveable), any(AABB.class), any())).thenAnswer(call ->
        {
            Predicate<? super Entity> filter = call.getArgument(2);
            return nearby.stream().filter(filter).toList();
        });

        SeatRiders.recall(driveable, new Seat[] {driverSeat, gunnerSeat});

        verify(gunner).startRiding(gunnerSeat);
        assertNull(((SeatRiderEntity) gunner).flansmodultimateSavedSeat());
        for (Entity other : List.of(stranger, latecomer, mounted, misplaced))
            verify(other, never()).startRiding(any());
        // A seat that no longer exists is forgotten rather than asked for again.
        assertNull(((SeatRiderEntity) misplaced).flansmodultimateSavedSeat());
        assertNotNull(((SeatRiderEntity) stranger).flansmodultimateSavedSeat());
    }

    private static Seat seat()
    {
        Seat seat = mock(Seat.class);
        when(seat.isAlive()).thenReturn(true);
        when(seat.getBoundingBox()).thenReturn(new AABB(0D, 0D, 0D, 1D, 1D, 1D));
        return seat;
    }

    /** An entity as the mixin leaves it: remembering the seat it was saved in. */
    private static Entity rider(SeatRiders.SavedSeat saved)
    {
        Entity entity = mock(LivingEntity.class, withSettings().extraInterfaces(SeatRiderEntity.class));
        SeatRiderEntity rider = (SeatRiderEntity) entity;
        SeatRiders.SavedSeat[] held = {saved};
        when(rider.flansmodultimateSavedSeat()).thenAnswer(call -> held[0]);
        doAnswer(call ->
        {
            held[0] = call.getArgument(0);
            return null;
        }).when(rider).flansmodultimateSetSavedSeat(any());
        when(entity.isAlive()).thenReturn(true);
        return entity;
    }
}
