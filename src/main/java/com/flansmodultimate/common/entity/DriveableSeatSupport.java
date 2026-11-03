package com.flansmodultimate.common.entity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/** Who occupies a driveable's controlling seat, for the public API. Seats are entities of their own, bound to the driveable rather than riding it. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DriveableSeatSupport
{
    public static Optional<LivingEntity> driver(Entity entity)
    {
        if (!(entity instanceof Driveable driveable))
            return Optional.empty();
        Seat seat = driveable.getDriverSeat();
        return seat != null && seat.getRiddenByEntity() instanceof LivingEntity living ? Optional.of(living) : Optional.empty();
    }
}
