package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.common.entity.Driveable;
import lombok.Setter;

import net.minecraft.core.BlockPos;

import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** Transfers supporting wheel contact to an optional moving-block physics provider. */
public final class VehicleDeckContact
{
    @Setter
    private static BiConsumer<Driveable, BlockPos> resolver = (vehicle, support) -> {};

    @Setter
    private static BiFunction<Driveable, BlockPos, Vec3> releaseMotion = (driveable, support) -> Vec3.ZERO;

    public static Vec3 release(Driveable driveable, BlockPos support)
    {
        return releaseMotion.apply(driveable, support);
    }

    private VehicleDeckContact() {}

    public static void apply(Driveable vehicle, BlockPos support)
    {
        resolver.accept(vehicle, support);
    }
}
