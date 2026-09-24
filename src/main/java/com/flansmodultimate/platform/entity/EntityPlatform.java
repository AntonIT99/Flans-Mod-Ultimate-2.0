package com.flansmodultimate.platform.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;

/** Version boundary for small entity API calls whose names or signatures changed. */
public final class EntityPlatform
{
    private EntityPlatform() {}

    /** Runs the mob's own spawn initialisation without spawn-group or NBT data. */
    @SuppressWarnings("deprecation") // NeoForge marks this as override-only; this keeps the direct call of 1.20.1.
    public static void finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType)
    {
        mob.finalizeSpawn(level, difficulty, spawnType, null);
    }

    public static void igniteForSeconds(Entity entity, int seconds)
    {
        entity.igniteForSeconds(seconds);
    }

    /** The player's connection latency in milliseconds. */
    public static int latency(ServerPlayer player)
    {
        return player.connection.latency();
    }

    /** Effective step height, including the step-height attribute of living entities. */
    public static float stepHeight(Entity entity)
    {
        return entity.maxUpStep();
    }
}
