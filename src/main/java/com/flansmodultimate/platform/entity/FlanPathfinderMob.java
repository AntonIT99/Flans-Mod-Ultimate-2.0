package com.flansmodultimate.platform.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** Version boundary for pathfinder-mob custom death loot. */
public abstract class FlanPathfinderMob extends PathfinderMob
{
    protected FlanPathfinderMob(EntityType<? extends PathfinderMob> type, Level level)
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

}
