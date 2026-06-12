package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketParticles implements IClientPacket
{
    private String particleType;
    private double x;
    private double y;
    private double z;
    private double dx;
    private double dy;
    private double dz;
    private float speed;
    private float scale;
    private int count;

    public PacketParticles(String particleType, Vec3 position, Vec3 delta, float speed, int count)
    {
        this(particleType, position, delta, speed, count, 1F);
    }

    public PacketParticles(String particleType, Vec3 position, Vec3 delta, float speed, int count, float scale)
    {
        this.particleType = particleType;
        x = position.x;
        y = position.y;
        z = position.z;
        dx = delta.x;
        dy = delta.y;
        dz = delta.z;
        this.speed = speed;
        this.count = count;
        this.scale = scale;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeUtf(particleType);
        data.writeDouble(x);
        data.writeDouble(y);
        data.writeDouble(z);
        data.writeDouble(dx);
        data.writeDouble(dy);
        data.writeDouble(dz);
        data.writeFloat(speed);
        data.writeFloat(scale);
        data.writeInt(count);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        particleType = data.readUtf();
        x = data.readDouble();
        y = data.readDouble();
        z = data.readDouble();
        dx = data.readDouble();
        dy = data.readDouble();
        dz = data.readDouble();
        speed = data.readFloat();
        scale = data.readFloat();
        count = data.readInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (count == 0)
        {
            ClientHooks.RENDER.spawnParticle(particleType, x, y, z, dx * speed, dy * speed, dz * speed, scale);
            return;
        }

        for (int i = 0; i < count; i++)
        {
            double ox = x + level.random.nextGaussian() * dx;
            double oy = y + level.random.nextGaussian() * dy;
            double oz = z + level.random.nextGaussian() * dz;
            double vx = level.random.nextGaussian() * speed;
            double vy = level.random.nextGaussian() * speed;
            double vz = level.random.nextGaussian() * speed;

            ClientHooks.RENDER.spawnParticle(particleType, ox, oy, oz, vx, vy, vz, scale);
        }
    }
}
