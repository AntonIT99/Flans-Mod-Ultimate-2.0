package com.flansmodultimate.platform.entity;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Loader boundary for entities that send extra data with their spawn packet. Entities implement the
 * {@link PacketBuffer} methods; the loader callbacks only adapt the buffer type.
 */
public interface SpawnDataEntity extends IEntityAdditionalSpawnData
{
    void writeSpawnData(PacketBuffer buffer);

    void readSpawnData(PacketBuffer buffer);

    @Override
    default void writeSpawnData(FriendlyByteBuf buffer)
    {
        writeSpawnData(new PacketBuffer(buffer));
    }

    @Override
    default void readSpawnData(FriendlyByteBuf buffer)
    {
        readSpawnData(new PacketBuffer(buffer));
    }
}
