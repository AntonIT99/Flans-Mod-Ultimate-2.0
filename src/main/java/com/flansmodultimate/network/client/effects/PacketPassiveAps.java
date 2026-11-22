package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A missile jammed by a vehicle's soft-kill protection: clients put it where the server jammed it and corkscrew it the same way */
@NoArgsConstructor
public class PacketPassiveAps implements IClientPacket
{
    private int entityId;
    private Vec3 position;
    private Vec3 velocity;
    private int seed;

    public PacketPassiveAps(int entityId, Vec3 position, Vec3 velocity, int seed)
    {
        this.entityId = entityId;
        this.position = position;
        this.velocity = velocity;
        this.seed = seed;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeDouble(velocity.x);
        data.writeDouble(velocity.y);
        data.writeDouble(velocity.z);
        data.writeInt(seed);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        velocity = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        seed = data.readInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(entityId) instanceof Bullet bullet && position.lengthSqr() < Double.MAX_VALUE && velocity.lengthSqr() < Double.MAX_VALUE)
            bullet.jamByPassiveAps(position, velocity, seed);
    }
}
