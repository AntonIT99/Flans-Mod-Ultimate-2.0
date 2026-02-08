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
import java.util.Optional;
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
    @Nullable
    private UUID playerUUID;

    public PacketPlaySound(Vec3 position, double range, @Nullable String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID, @Nullable Player source)
    {
        posX = (float) position.x;
        posY = (float) position.y;
        posZ = (float) position.z;
        this.range = (float) range;
        this.sound = Objects.requireNonNullElse(sound, StringUtils.EMPTY);
        this.distort = distort;
        this.silenced = silenced;
        this.cancellable = cancellable;
        this.instanceUUID = instanceUUID;
        playerUUID = source != null ? source.getUUID() : null;
    }

    public PacketPlaySound(Vec3 position, double range, @Nullable String sound, boolean distort, boolean silenced, @Nullable Player source)
    {
        this(position, range, sound, distort, silenced, false, UUID.randomUUID(), source);
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
        data.writeOptional(Optional.ofNullable(playerUUID), FriendlyByteBuf::writeUUID);
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
        playerUUID = data.readOptional(FriendlyByteBuf::readUUID).orElse(null);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.SOUND.playSound(sound, new Vec3(posX, posY, posZ), range, distort, silenced, cancellable, instanceUUID, playerUUID != null ? level.getPlayerByUUID(playerUUID) : null);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID, @Nullable Player player)
    {
        // Minimum Range of 16
        PacketHandler.sendToAllAround(new PacketPlaySound(position, Math.max(16, range), sound, distort, silenced, cancellable, instanceUUID, player), position, Math.max(16, range), dimension);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced, @Nullable Player player)
    {
        sendSoundPacket(position, range, dimension, sound, distort, silenced, false, UUID.randomUUID(), player);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort, @Nullable Player player)
    {
        sendSoundPacket(position, range, dimension, sound, distort, false, player);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        sendSoundPacket(entity.position(), range, entity.level().dimension(), sound, distort, silenced, cancellable, instanceUUID, entity instanceof Player player ? player : null);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort, boolean silenced)
    {
        sendSoundPacket(entity.position(), range, entity.level().dimension(), sound, distort, silenced, entity instanceof Player player ? player : null);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort)
    {
        sendSoundPacket(entity.position(), range, entity.level().dimension(), sound, distort, false, entity instanceof Player player ? player : null);
    }
}
