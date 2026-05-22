package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanParticles;
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
public class PacketFlanExplosionParticles implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketFlanExplosionParticles> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "flan_explosion_particles"));

    public static final StreamCodec<FriendlyByteBuf, PacketFlanExplosionParticles> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketFlanExplosionParticles decode(FriendlyByteBuf buf)
        {
            PacketFlanExplosionParticles packet = new PacketFlanExplosionParticles();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketFlanExplosionParticles packet)
        {
            packet.encodeInto(buf);
        }
    };

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
    public void handleClientSide()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        spawn(level, FlanParticles.FM_FLARE, position, numSmoke, radius * 0.1F);
        spawn(level, FlanParticles.FM_DEBRIS_1, position, numDebris, radius * 0.1F);
    }

    private void spawn(ClientLevel level, String particleType, Vec3 position, int count, float maxVelocity)
    {
        for (int i = 0; i < count; i++)
        {
            float vx = (level.random.nextFloat() * 2.0F - 1.0F) * maxVelocity;
            float vy = level.random.nextFloat() * maxVelocity;
            float vz = (level.random.nextFloat() * 2.0F - 1.0F) * maxVelocity;
            ClientHooks.RENDER.spawnParticle(particleType, position.x, position.y, position.z, vx, vy, vz, 1F);
        }
    }
}