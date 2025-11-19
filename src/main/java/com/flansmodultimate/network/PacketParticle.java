package com.flansmodultimate.network;

import com.flansmodultimate.client.render.ParticleHelper;
import lombok.NoArgsConstructor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

@NoArgsConstructor
public class PacketParticle implements IClientPacket
{
    private float x;
    private float y;
    private float z;
    private float mx;
    private float my;
    private float mz;
    private float scale;
    private String particleType;

    public PacketParticle(String s, double x1, double y1, double z1, double x2, double y2, double z2)
    {
        this(s, x1, y1, z1, x2, y2, z2, 1.0F);
    }

    public PacketParticle(String s, double x1, double y1, double z1, double x2, double y2, double z2, float size)
    {
        x = (float) x1;
        y = (float) y1;
        z = (float) z1;
        mx = (float) x2;
        my = (float) y2;
        mz = (float) z2;
        particleType = s;
        scale = size;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeFloat(z);
        data.writeFloat(mx);
        data.writeFloat(my);
        data.writeFloat(mz);
        data.writeFloat(scale);
        data.writeUtf(particleType);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        x = data.readFloat();
        y = data.readFloat();
        z = data.readFloat();
        mx = data.readFloat();
        my = data.readFloat();
        mz = data.readFloat();
        scale = data.readFloat();
        particleType = data.readUtf();
    }

    @Override
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        //TODO: take scale into account
        ParticleHelper.spawnFromString(level, particleType, x, y, z, mx, my, mz);
    }
}
