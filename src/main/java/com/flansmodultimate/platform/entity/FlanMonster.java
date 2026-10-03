package com.flansmodultimate.platform.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** Version boundary for mob spawn initialization and custom death loot. */
public abstract class FlanMonster extends Monster
{
    protected FlanMonster(EntityType<? extends Monster> type, Level level)
    {
        super(type, level);
    }

    @Override
    protected final void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        dropEntityDeathLoot(source, recentlyHit);
    }

    protected void dropEntityDeathLoot(DamageSource source, boolean recentlyHit) {}

    @Override
    public final SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnData, CompoundTag dataTag)
    {
        return finalizeEntitySpawn(level, difficulty, spawnType, spawnData, dataTag);
    }

    @SuppressWarnings("deprecation") // Calls the vanilla superclass implementation from the loader lifecycle.
    protected SpawnGroupData finalizeEntitySpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
        MobSpawnType spawnType, SpawnGroupData spawnData, CompoundTag dataTag)
    {
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
    }
}
