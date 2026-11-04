package com.wolffsmod.npcs.drivers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Short server-thread leases while soldiers walk to seats; each level owns its own table. */
final class SeatReservations
{
    private static final long LEASE_TICKS = 40L;
    private final Map<UUID, Lease> seats = new HashMap<>();

    private record Lease(UUID rider, long until)
    {}

    boolean available(UUID seat, UUID rider, long now)
    {
        expire(now);
        Lease lease = seats.get(seat);
        return lease == null || lease.rider().equals(rider);
    }

    boolean reserve(UUID seat, UUID rider, long now)
    {
        if (!available(seat, rider, now))
            return false;
        release(rider);
        seats.put(seat, new Lease(rider, now + LEASE_TICKS));
        return true;
    }

    Optional<UUID> seatOf(UUID rider, long now)
    {
        expire(now);
        return seats.entrySet().stream().filter(entry -> entry.getValue().rider().equals(rider)).map(Map.Entry::getKey).findFirst();
    }

    void release(UUID rider)
    {
        seats.values().removeIf(lease -> lease.rider().equals(rider));
    }

    private void expire(long now)
    {
        seats.values().removeIf(lease -> lease.until() <= now);
    }
}
