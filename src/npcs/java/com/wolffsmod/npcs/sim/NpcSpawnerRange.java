package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.NpcsCommonConfig;
import com.wolffsmod.npcs.combat.NpcAirCombat;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

import java.util.function.IntSupplier;

/**
 * How far a creature made by a Custom NPCs spawner job may move from the spawner before the spawner removes it. Custom NPCs
 * keeps everything within 60 blocks, which an aircraft leaves on its first pass over a target and a vehicle while it
 * manoeuvres round one, so each kind gets a range of its own: the spawner's, set on its job page, or else the server's.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSpawnerRange
{
    /** Most blocks a range may be set to, on a spawner or in the configuration. */
    public static final int MAX = 1024;
    /** Saved with the spawner job: its own range of each kind, in the order of {@link Kind}, 0 for the server default. */
    private static final String NBT_RANGES = "WolffsModSpawnerRanges";

    /** What a spawned creature is, as far as its range goes. Saves hold the ranges in this order. */
    public enum Kind
    {
        GENERAL(NpcsCommonConfig::spawnerRange), VEHICLE(NpcsCommonConfig::spawnerVehicleRange), AIRCRAFT(NpcsCommonConfig::spawnerAircraftRange);

        private final IntSupplier serverDefault;

        Kind(IntSupplier serverDefault)
        {
            this.serverDefault = serverDefault;
        }

        /** @return the range of this kind the configuration sets for every spawner that keeps none of its own */
        public int serverDefault()
        {
            return serverDefault.getAsInt();
        }
    }

    /** @return blocks along each axis the spawned creature may move from its spawner */
    public static int of(SpawnerRanges spawner, Entity spawned)
    {
        Kind kind = kind(spawned);
        int own = spawner.wolffsmodnpcsSpawnerRange(kind);
        return own > 0 ? own : kind.serverDefault();
    }

    static Kind kind(Entity spawned)
    {
        if (NpcAirCombat.aircraft(spawned))
            return Kind.AIRCRAFT;
        return NpcVehicleThreats.type(spawned).isPresent() ? Kind.VEHICLE : Kind.GENERAL;
    }

    /** @return a range as a spawner keeps it: 0 for the server default, otherwise at most {@link #MAX} */
    public static int clamp(int blocks)
    {
        return blocks <= 0 ? 0 : Math.min(blocks, MAX);
    }

    public static void save(SpawnerRanges spawner, CompoundTag tag)
    {
        Kind[] kinds = Kind.values();
        int[] ranges = new int[kinds.length];
        for (Kind kind : kinds)
            ranges[kind.ordinal()] = spawner.wolffsmodnpcsSpawnerRange(kind);
        tag.putIntArray(NBT_RANGES, ranges);
    }

    /** Reads the spawner's own ranges; a job saved without them takes every server default. */
    public static void load(SpawnerRanges spawner, CompoundTag tag)
    {
        int[] ranges = tag.contains(NBT_RANGES, Tag.TAG_INT_ARRAY) ? tag.getIntArray(NBT_RANGES) : new int[0];
        for (Kind kind : Kind.values())
            spawner.wolffsmodnpcsSetSpawnerRange(kind, kind.ordinal() < ranges.length ? ranges[kind.ordinal()] : 0);
    }
}
