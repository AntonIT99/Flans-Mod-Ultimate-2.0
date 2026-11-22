package com.flansmodultimate.common.entity.geometry;

import lombok.Setter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiFunction;

/** Coordinates of the blocks supporting a stationary emplacement. Ordinary terrain uses the identity frame. */
public interface EmplacementFrame
{
    EmplacementFrame TERRAIN = new EmplacementFrame() {};

    final class Lookup
    {
        @Setter
        private static BiFunction<Level, BlockPos, EmplacementFrame> resolver = (level, pos) -> TERRAIN;

        private Lookup() {}
    }

    static EmplacementFrame at(Level level, BlockPos pos)
    {
        return Lookup.resolver.apply(level, pos);
    }

    default java.util.UUID deckId() { return null; }

    default Vec3 worldPosition(Vec3 local) { return local; }
    default Vec3 localPosition(Vec3 world) { return world; }
    default Vec3 worldDirection(Vec3 local) { return local; }
    default Vec3 localDirection(Vec3 world) { return world; }
}
