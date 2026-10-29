package com.wolffsmod.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/** A successful mounted shot; offset and direction use the model preview frame before NPC size and hull yaw. */
public record MuzzleFlashPacket(int entityId, String weapon, Vec3 offset, Vec3 direction)
{
    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeUtf(weapon);
        buffer.writeDouble(offset.x);
        buffer.writeDouble(offset.y);
        buffer.writeDouble(offset.z);
        buffer.writeDouble(direction.x);
        buffer.writeDouble(direction.y);
        buffer.writeDouble(direction.z);
    }

    public static MuzzleFlashPacket read(FriendlyByteBuf buffer)
    {
        return new MuzzleFlashPacket(buffer.readVarInt(), buffer.readUtf(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
            new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }
}
