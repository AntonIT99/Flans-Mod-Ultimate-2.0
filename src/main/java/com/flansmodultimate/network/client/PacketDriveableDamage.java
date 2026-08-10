package com.flansmodultimate.network.client;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.network.IClientPacket;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/** Compact authoritative snapshot of the driveable parts that changed this tick. */
public final class PacketDriveableDamage implements IClientPacket
{
    private int driveableId;
    private int[] partOrdinals = new int[0];
    private float[] health = new float[0];
    private int[] fireTicks = new int[0];
    private byte[] flags = new byte[0];

    public PacketDriveableDamage() {}

    public PacketDriveableDamage(int driveableId, @NotNull List<DriveablePart> parts)
    {
        this.driveableId = driveableId;
        int count = Math.min(parts.size(), EnumDriveablePart.values().length);
        partOrdinals = new int[count];
        health = new float[count];
        fireTicks = new int[count];
        flags = new byte[count];
        for (int index = 0; index < count; index++)
        {
            DriveablePart part = parts.get(index);
            partOrdinals[index] = part.getType().ordinal();
            health[index] = part.getHealth();
            fireTicks[index] = part.getFireTime();
            flags[index] = (byte) ((part.isOnFire() ? 1 : 0) | (part.isDead() ? 2 : 0));
        }
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeVarInt(driveableId);
        buffer.writeVarInt(partOrdinals.length);
        for (int index = 0; index < partOrdinals.length; index++)
        {
            buffer.writeVarInt(partOrdinals[index]);
            buffer.writeFloat(health[index]);
            buffer.writeVarInt(fireTicks[index]);
            buffer.writeByte(flags[index]);
        }
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf buffer)
    {
        driveableId = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > EnumDriveablePart.values().length)
            throw new IllegalArgumentException("Invalid driveable part count " + count);
        partOrdinals = new int[count];
        health = new float[count];
        fireTicks = new int[count];
        flags = new byte[count];
        for (int index = 0; index < count; index++)
        {
            partOrdinals[index] = buffer.readVarInt();
            health[index] = buffer.readFloat();
            fireTicks[index] = buffer.readVarInt();
            flags[index] = buffer.readByte();
        }
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Entity entity = level.getEntity(driveableId);
        if (entity instanceof Driveable driveable)
            driveable.applyPartNetworkState(partOrdinals, health, fireTicks, flags);
    }
}
