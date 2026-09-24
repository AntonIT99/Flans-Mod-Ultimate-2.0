package com.flansmodultimate.platform.entity;

import com.flansmodultimate.network.PacketBuffer;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Loader boundary for entities that send extra data with their spawn packet. Entities implement the
 * {@link PacketBuffer} methods; the loader callbacks only adapt the buffer type.
 */
public interface SpawnDataEntity extends IEntityWithComplexSpawn
{
    void writeSpawnData(PacketBuffer buffer);

    void readSpawnData(PacketBuffer buffer);

    @Override
    default void writeSpawnData(RegistryFriendlyByteBuf buffer)
    {
        writeSpawnData(new PacketBuffer(buffer));
    }

    @Override
    default void readSpawnData(RegistryFriendlyByteBuf buffer)
    {
        readSpawnData(new PacketBuffer(buffer));
    }
}
