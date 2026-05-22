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
public class PacketExplodeParticles implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketExplodeParticles> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "explode_particles"));

    public static final StreamCodec<FriendlyByteBuf, PacketExplodeParticles> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketExplodeParticles decode(FriendlyByteBuf buf)
        {
            PacketExplodeParticles packet = new PacketExplodeParticles();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketExplodeParticles packet)
        {
            packet.encodeInto(buf);
        }
    };

    private String particleType;
    private int number;
    private float x;
    private float y;
    private float z;

    public PacketExplodeParticles(String particleType, int number, Vec3 position)
    {
        this.particleType = particleType;
        this.number = number;
        x = (float) position.x;
        y = (float) position.y;
        z = (float) position.z;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
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
    public void handleClientSide()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        for (int i = 0; i < number; i++)
            ClientHooks.RENDER.spawnParticle(particleType, x, y, z, level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian(), 1F);
    }
}