package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketBulletTrail implements IClientPacket
{
    public static final CustomPacketPayload.Type<PacketBulletTrail> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "bullet_trail"));

    public static final StreamCodec<FriendlyByteBuf, PacketBulletTrail> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketBulletTrail decode(FriendlyByteBuf buf)
        {
            PacketBulletTrail packet = new PacketBulletTrail();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketBulletTrail packet)
        {
            packet.encodeInto(buf);
        }
    };

    private Vec3 origin;
    private Vec3 hitPos;
    private float width;
    private float length;
    private float bulletSpeed;
    private String trailTexture;

    public PacketBulletTrail(Vec3 origin, Vec3 hitPos, float width, float length, float bulletSpeed, String trailTexture)
    {
        this.origin = origin;
        this.hitPos = hitPos;
        this.width = width;
        this.length = length;
        this.bulletSpeed = bulletSpeed;
        this.trailTexture = trailTexture;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat((float) origin.x);
        data.writeFloat((float) origin.y);
        data.writeFloat((float) origin.z);
        data.writeFloat((float) hitPos.x);
        data.writeFloat((float) hitPos.y);
        data.writeFloat((float) hitPos.z);
        data.writeFloat(width);
        data.writeFloat(length);
        data.writeFloat(bulletSpeed);
        data.writeUtf(trailTexture);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        origin = new Vec3(data.readFloat(), data.readFloat(), data.readFloat());
        hitPos = new Vec3(data.readFloat(), data.readFloat(), data.readFloat());
        width = data.readFloat();
        length = data.readFloat();
        bulletSpeed = data.readFloat();
        trailTexture = data.readUtf();
    }

    @Override
    public void handleClientSide()
    {
        ClientHooks.RENDER.spawnTrail(trailTexture, origin, hitPos, width, length, bulletSpeed);
    }
}