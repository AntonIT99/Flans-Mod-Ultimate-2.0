package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A round deflected by a melee block: clients move it to the deflection point and send it on its new course */
@NoArgsConstructor
public class PacketBulletDeflect implements IClientPacket
{
    private int entityId;
    private Vec3 position;
    private Vec3 velocity;

    public PacketBulletDeflect(int entityId, Vec3 position, Vec3 velocity)
    {
        this.entityId = entityId;
        this.position = position;
        this.velocity = velocity;
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
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        velocity = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(entityId) instanceof Bullet bullet)
            bullet.applyDeflection(position, velocity);
    }
}
