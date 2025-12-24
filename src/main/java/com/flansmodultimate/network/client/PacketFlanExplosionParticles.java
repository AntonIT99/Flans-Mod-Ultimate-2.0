package com.flansmodultimate.network.client;

import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketFlanExplosionParticles implements IClientPacket
{
    private Vec3 position;
    private int numSmoke;
    private int numDebris;
    private float radius;

    public PacketFlanExplosionParticles(Vec3 position, int numSmoke, int numDebris, float radius)
    {
        this.position = position;
        this.numSmoke = numSmoke;
        this.numDebris = numDebris;
        this.radius = radius;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeInt(numSmoke);
        data.writeInt(numDebris);
        data.writeFloat(radius);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        numSmoke = data.readInt();
        numDebris = data.readInt();
        radius = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        spawn(level, ParticleHelper.FM_FLARE, position, numSmoke, radius * 0.1F);
        spawn(level, ParticleHelper.FM_DEBRIS_1, position, numDebris, radius * 0.1F);
    }

    private void spawn(Level level, String particleType, Vec3 position, int count, float maxVelocity)
    {
        for (int i = 0; i < count; i++)
        {
            float vx = (level.random.nextFloat() * 2.0F - 1.0F) * maxVelocity;
            float vy = level.random.nextFloat() * maxVelocity;
            float vz = (level.random.nextFloat() * 2.0F - 1.0F) * maxVelocity;
            ParticleHelper.spawnFromString(particleType, position.x, position.y, position.z, vx, vy, vz, 1F);
        }
    }
}
