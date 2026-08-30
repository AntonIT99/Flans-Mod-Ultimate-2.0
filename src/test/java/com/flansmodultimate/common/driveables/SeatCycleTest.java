package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeatCycleTest
{
    @Test
    void cyclesInOrderAndWrapsToDriver()
    {
        assertEquals(1, SeatCycle.nextAvailable(0, 4, index -> true));
        assertEquals(0, SeatCycle.nextAvailable(3, 4, index -> true));
    }

    @Test
    void skipsOccupiedOrBrokenSeats()
    {
        boolean[] available = {true, false, false, true};
        assertEquals(3, SeatCycle.nextAvailable(0, available.length, index -> available[index]));
        assertEquals(0, SeatCycle.nextAvailable(3, available.length, index -> available[index]));
    }

    @Test
    void staysPutWhenNoOtherSeatIsFree()
    {
        assertEquals(-1, SeatCycle.nextAvailable(1, 3, index -> index == 1));
    }
}
