package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor
public class PacketManualGuidance implements IServerPacket
{
    public static final CustomPacketPayload.Type<PacketManualGuidance> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "manual_guidance"));

    public static final StreamCodec<FriendlyByteBuf, PacketManualGuidance> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public PacketManualGuidance decode(FriendlyByteBuf buf)
        {
            PacketManualGuidance packet = new PacketManualGuidance();
            packet.decodeInto(buf);
            return packet;
        }

        @Override
        public void encode(FriendlyByteBuf buf, PacketManualGuidance packet)
        {
            packet.encodeInto(buf);
        }
    };

    private int bulletEntityId;

    private float originX;
    private float originY;
    private float originZ;

    private float lookX;
    private float lookY;
    private float lookZ;

    public PacketManualGuidance(int entityId, float originX, float originY, float originZ, float lookX, float lookY, float lookZ)
    {
        bulletEntityId = entityId;

        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;

        this.lookX = lookX;
        this.lookY = lookY;
        this.lookZ = lookZ;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeInt(bulletEntityId);

        data.writeFloat(originX);
        data.writeFloat(originY);
        data.writeFloat(originZ);

        data.writeFloat(lookX);
        data.writeFloat(lookY);
        data.writeFloat(lookZ);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        bulletEntityId = data.readInt();

        originX = data.readFloat();
        originY = data.readFloat();
        originZ = data.readFloat();

        lookX = data.readFloat();
        lookY = data.readFloat();
        lookZ = data.readFloat();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world)
    {
        if (world.getEntity(bulletEntityId) instanceof Bullet bullet)
        {
            LivingEntity owner = bullet.getOwner().orElse(null);
            if (player != owner)
                return;

            bullet.setOwnerPos(new Vec3(originX, originY, originZ));
            bullet.setOwnerLook(new Vec3(lookX, lookY, lookZ));
        }
    }
}