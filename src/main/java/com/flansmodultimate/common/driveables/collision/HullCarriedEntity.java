package com.flansmodultimate.common.driveables.collision;

/** Gives every {@code Entity} its {@link HullCarry} record; implemented by {@code EntityMixin}. */
public interface HullCarriedEntity
{
    HullCarry flansmodultimateHullCarry();

    /** @return the record, or null while no hull has carried the entity */
    HullCarry flansmodultimateHullCarryIfPresent();
}
