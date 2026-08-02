package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.entity.Flag;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/** A capturable arena base, currently implemented by flagpoles. */
public interface ITeamBase extends ITeamObject
{
    String getBaseName();
    void setBaseName(String name);
    int getDefaultOwnerId();
    void setDefaultOwnerId(int id);
    int getOwnerId();
    void setOwnerId(int id);
    String getMapId();
    void setMapId(String mapId);
    Collection<UUID> getObjectIds();
    void addObject(UUID objectId);
    void removeObject(UUID objectId);
    void startRound();
    void roundCleanup();
    @Nullable Flag getFlag();

    @Override
    default UUID getBaseId()
    {
        return getObjectId();
    }

    @Override
    default void setBaseId(@Nullable UUID ignored) {}

    @Override
    default boolean isSpawnPoint()
    {
        return false;
    }
}
