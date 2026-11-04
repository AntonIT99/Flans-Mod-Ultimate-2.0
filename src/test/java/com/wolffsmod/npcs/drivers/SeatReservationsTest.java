package com.wolffsmod.npcs.drivers;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SeatReservationsTest
{
    @Test
    void approachingSoldiersCannotReserveTheSameSeatAndInterruptedLeasesExpire()
    {
        SeatReservations reservations = new SeatReservations();
        UUID seat = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertTrue(reservations.reserve(seat, first, 100L));
        assertFalse(reservations.reserve(seat, second, 101L));
        assertTrue(reservations.reserve(seat, first, 120L));
        assertFalse(reservations.available(seat, second, 159L));
        assertTrue(reservations.reserve(seat, second, 160L));
        assertTrue(reservations.seatOf(first, 160L).isEmpty());
    }

    @Test
    void changingSeatsOrStoppingReleasesThePreviousSeat()
    {
        SeatReservations reservations = new SeatReservations();
        UUID firstSeat = UUID.randomUUID();
        UUID secondSeat = UUID.randomUUID();
        UUID rider = UUID.randomUUID();
        assertTrue(reservations.reserve(firstSeat, rider, 0L));
        assertTrue(reservations.reserve(secondSeat, rider, 1L));
        assertTrue(reservations.available(firstSeat, UUID.randomUUID(), 2L));
        assertEquals(secondSeat, reservations.seatOf(rider, 2L).orElseThrow());
        reservations.release(rider);
        assertTrue(reservations.seatOf(rider, 2L).isEmpty());
        assertTrue(reservations.available(secondSeat, UUID.randomUUID(), 2L));
    }
}
