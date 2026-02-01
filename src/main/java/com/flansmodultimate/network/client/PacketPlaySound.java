package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
public class PacketPlaySound implements IClientPacket
{
    private float posX;
    private float posY;
    private float posZ;
    private float range;
    private String sound;
    private boolean distort;
    private boolean silenced;
    private boolean cancellable;
    private UUID instanceUUID;

    public PacketPlaySound(double x, double y, double z, double range, @Nullable String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        posX = (float) x;
        posY = (float) y;
        posZ = (float) z;
        this.range = (float) range;
        this.sound = Objects.requireNonNullElse(sound, StringUtils.EMPTY);
        this.distort = distort;
        this.silenced = silenced;
        this.cancellable = cancellable;
        this.instanceUUID = instanceUUID;
    }

    public PacketPlaySound(double x, double y, double z, double range, @Nullable String sound, boolean distort, boolean silenced)
    {
        this(x, y, z, range, sound, distort, silenced, false, UUID.randomUUID());
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(posX);
        data.writeFloat(posY);
        data.writeFloat(posZ);
        data.writeFloat(range);
        data.writeUtf(sound);
        data.writeBoolean(distort);
        data.writeBoolean(silenced);
        data.writeBoolean(cancellable);
        data.writeUUID(instanceUUID);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        posX = data.readFloat();
        posY = data.readFloat();
        posZ = data.readFloat();
        range = data.readFloat();
        sound = data.readUtf();
        distort = data.readBoolean();
        silenced = data.readBoolean();
        cancellable = data.readBoolean();
        instanceUUID = data.readUUID();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.SOUND.playSound(sound, new Vec3(posX, posY, posZ), range, distort, silenced, cancellable, instanceUUID);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        PacketHandler.sendToAllAround(new PacketPlaySound(x, y, z, range, sound, distort, silenced, cancellable, instanceUUID), x, y, z, (float) range, dimension);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced)
    {
        PacketHandler.sendToAllAround(new PacketPlaySound(x, y, z, range, sound, distort, silenced, false, UUID.randomUUID()), x, y, z, (float) range, dimension);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort)
    {
        sendSoundPacket(x, y, z, range, dimension, sound, distort, false);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        sendSoundPacket(position.x, position.y, position.z, range, dimension, sound, distort, silenced, cancellable, instanceUUID);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced)
    {
        sendSoundPacket(position.x, position.y, position.z, range, dimension, sound, distort, silenced);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort)
    {
        sendSoundPacket(position.x, position.y, position.z, range, dimension, sound, distort);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        sendSoundPacket(entity.getX(), entity.getY(), entity.getZ(), range, entity.level().dimension(), sound, distort, silenced, cancellable, instanceUUID);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort, boolean silenced)
    {
        sendSoundPacket(entity.getX(), entity.getY(), entity.getZ(), range, entity.level().dimension(), sound, distort, silenced);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort)
    {
        sendSoundPacket(entity.getX(), entity.getY(), entity.getZ(), range, entity.level().dimension(), sound, distort, false);
    }
}
