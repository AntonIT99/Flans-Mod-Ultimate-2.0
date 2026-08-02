package com.flansmodultimate.common.teams;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Common contract for persistent arena objects. */
public interface ITeamObject
{
    UUID getObjectId();
    ResourceKey<Level> getDimension();
    Vec3 getTeamObjectPosition();
    @Nullable UUID getBaseId();
    void setBaseId(@Nullable UUID baseId);
    boolean isSpawnPoint();
    void destroyTeamObject();
}
