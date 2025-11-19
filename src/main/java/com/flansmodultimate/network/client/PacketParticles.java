package com.flansmodultimate.network.client;

import com.flansmodultimate.client.render.ParticleHelper;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketParticles implements IClientPacket
{
    private String particleType;
    private int number;
    private float x;
    private float y;
    private float z;

    public PacketParticles(String particleType, int number, Vec3 position)
    {
        this.particleType = particleType;
        this.number = number;
        x = (float) position.x;
        y = (float) position.y;
        z = (float) position.z;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeUtf(particleType);
        data.writeInt(number);
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeFloat(z);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        particleType = data.readUtf();
        number = data.readInt();
        x = data.readFloat();
        y = data.readFloat();
        z = data.readFloat();
    }

    @Override
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        for (int i = 0; i < number; i++)
            ParticleHelper.spawnFromString(level, particleType, x, y, z, level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian());
    }
}
