package com.flansmodultimate.network;

import com.flansmodultimate.client.render.ParticleHelper;
import lombok.NoArgsConstructor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

@NoArgsConstructor
public class PacketFlak implements IClientPacket
{
    /** Position of this flak */
    private double x;
    private double y;
    private double z;
    /** Num particles */
    private int numParticles;
    /** Particle type */
    private String particleType;

    public PacketFlak(double x1, double y1, double z1, int n, String s)
    {
        x = x1;
        y = y1;
        z = z1;
        numParticles = n;
        particleType = s;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeDouble(x);
        data.writeDouble(y);
        data.writeDouble(z);
        data.writeInt(numParticles);
        data.writeUtf(particleType);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        x = data.readDouble();
        y = data.readDouble();
        z = data.readDouble();
        numParticles = data.readInt();
        particleType = data.readUtf();
    }

    @Override
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        for (int i = 0; i < numParticles; i++)
        {
            double ox = x + level.random.nextGaussian();
            double oy = y + level.random.nextGaussian();
            double oz = z + level.random.nextGaussian();
            double vx = level.random.nextGaussian() / 20.0;
            double vy = level.random.nextGaussian() / 20.0;
            double vz = level.random.nextGaussian() / 20.0;

            ParticleHelper.spawnFromString(level, particleType, ox, oy, oz, vx, vy, vz);
        }
    }
}
