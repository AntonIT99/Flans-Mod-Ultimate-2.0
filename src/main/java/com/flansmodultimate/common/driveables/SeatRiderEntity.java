package com.flansmodultimate.common.driveables;

import org.jetbrains.annotations.Nullable;

/** Gives every {@code Entity} the seat it was saved in, if any; implemented by {@code EntityMixin}. */
public interface SeatRiderEntity
{
    /** @return the seat this entity rode when it was saved, or null once it has been called back or never rode one */
    @Nullable
    SeatRiders.SavedSeat flansmodultimateSavedSeat();

    void flansmodultimateSetSavedSeat(@Nullable SeatRiders.SavedSeat seat);
}
