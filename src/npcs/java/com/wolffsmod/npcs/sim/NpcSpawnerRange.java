package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.NpcsCommonConfig;
import com.wolffsmod.npcs.combat.NpcAirCombat;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;

/**
 * How far a creature made by a Custom NPCs spawner job may move from the spawner before the spawner removes it. Custom NPCs
 * keeps everything within 60 blocks, which an aircraft leaves on its first pass over a target and a vehicle while it
 * manoeuvres round one, so each gets a range of its own.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSpawnerRange
{
    /** @return blocks along each axis the spawned creature may move from its spawner */
    public static int of(Entity spawned)
    {
        if (NpcAirCombat.aircraft(spawned))
            return NpcsCommonConfig.spawnerAircraftRange();
        return NpcVehicleThreats.type(spawned).isPresent() ? NpcsCommonConfig.spawnerVehicleRange() : NpcsCommonConfig.spawnerRange();
    }
}
