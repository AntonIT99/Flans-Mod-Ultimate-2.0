package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.common.entity.Vehicle;
import lombok.Setter;

import net.minecraft.core.BlockPos;

import java.util.function.BiConsumer;

/** Transfers supporting wheel contact to an optional moving-block physics provider. */
public final class VehicleDeckContact
{
    @Setter
    private static BiConsumer<Vehicle, BlockPos> resolver = (vehicle, support) -> {};

    private VehicleDeckContact() {}

    public static void apply(Vehicle vehicle, BlockPos support)
    {
        resolver.accept(vehicle, support);
    }
}
