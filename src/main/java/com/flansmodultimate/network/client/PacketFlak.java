package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketFlak implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketFlak> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "flak"));

    public static final StreamCodec<FriendlyByteBuf, PacketFlak> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketFlak decode(FriendlyByteBuf buf)
        {
            PacketFlak packet = new PacketFlak();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketFlak packet)
        {
            packet.encodeInto(buf);
        }
    };

    private Vec3 position;
    private int numParticles;
    private String particleType;

    public PacketFlak(Vec3 position, int n, String s)
    {
        this.position = position;
        numParticles = n;
        particleType = s;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeDouble(position.x);
        data.writeDouble(position.y);
        data.writeDouble(position.z);
        data.writeInt(numParticles);
        data.writeUtf(particleType);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        position = new Vec3(data.readDouble(), data.readDouble(), data.readDouble());
        numParticles = data.readInt();
        particleType = data.readUtf();
    }

    @Override
    public void handleClientSide()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        for (int i = 0; i < numParticles; i++)
        {
            double ox = position.x + level.random.nextGaussian();
            double oy = position.y + level.random.nextGaussian();
            double oz = position.z + level.random.nextGaussian();
            double vx = level.random.nextGaussian() / 20.0;
            double vy = level.random.nextGaussian() / 20.0;
            double vz = level.random.nextGaussian() / 20.0;

            ClientHooks.RENDER.spawnParticle(particleType, ox, oy, oz, vx, vy, vz, 1F);
        }
    }
}