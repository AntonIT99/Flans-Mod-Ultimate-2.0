package com.flansmodultimate.network.client.driveable;

import com.flansmodultimate.client.distant.DistantContactsClient;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * The driveables a player is too far away to track, sent every few ticks to players who draw far terrain, so
 * their client can show them as simplified shapes. Display data only: it carries no state the client may
 * act on, and every send replaces the previous one.
 */
@NoArgsConstructor
public class PacketDistantContacts implements IClientPacket
{
    /** More than any server sends; bounds what a malformed packet can make the client allocate. */
    private static final int MAX_CONTACTS = 1024;
    private static final int MAX_DESTROYED_PARTS = 256;
    private static final int MAX_SHORT_NAME_LENGTH = 256;

    /**
     * One distant driveable. Angles are the driveable's own yaw, pitch and roll in degrees, in the legacy model
     * basis; velocity is in blocks per tick.
     */
    public record Contact(int entityId, String shortName, int paintjobId, double x, double y, double z, float velocityX, float velocityY, float velocityZ, float yaw, float pitch, float roll,
        int[] destroyedParts)
    {}

    /** Ticks until the server sends the next update, which bounds how far the client extrapolates. */
    @Getter
    private int updateInterval;
    @Getter
    private List<Contact> contacts = List.of();

    public PacketDistantContacts(int updateInterval, List<Contact> contacts)
    {
        this.updateInterval = updateInterval;
        this.contacts = List.copyOf(contacts);
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(updateInterval);
        data.writeVarInt(contacts.size());
        for (Contact contact : contacts)
        {
            data.writeVarInt(contact.entityId());
            data.writeUtf(contact.shortName(), MAX_SHORT_NAME_LENGTH);
            data.writeVarInt(contact.paintjobId());
            data.writeDouble(contact.x());
            data.writeDouble(contact.y());
            data.writeDouble(contact.z());
            data.writeFloat(contact.velocityX());
            data.writeFloat(contact.velocityY());
            data.writeFloat(contact.velocityZ());
            data.writeFloat(contact.yaw());
            data.writeFloat(contact.pitch());
            data.writeFloat(contact.roll());
            data.writeVarInt(contact.destroyedParts().length);
            for (int part : contact.destroyedParts())
                data.writeVarInt(part);
        }
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        updateInterval = data.readVarInt();
        int count = data.readVarInt();
        if (count < 0 || count > MAX_CONTACTS)
            throw new IllegalStateException("Too many distant contacts: " + count);

        List<Contact> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            int entityId = data.readVarInt();
            String shortName = data.readUtf(MAX_SHORT_NAME_LENGTH);
            int paintjobId = data.readVarInt();
            double x = data.readDouble();
            double y = data.readDouble();
            double z = data.readDouble();
            float velocityX = data.readFloat();
            float velocityY = data.readFloat();
            float velocityZ = data.readFloat();
            float yaw = data.readFloat();
            float pitch = data.readFloat();
            float roll = data.readFloat();
            int destroyedCount = data.readVarInt();
            if (destroyedCount < 0 || destroyedCount > MAX_DESTROYED_PARTS)
                throw new IllegalStateException("Too many destroyed parts: " + destroyedCount);
            int[] destroyed = new int[destroyedCount];
            for (int part = 0; part < destroyedCount; part++)
                destroyed[part] = data.readVarInt();
            decoded.add(new Contact(entityId, shortName, paintjobId, x, y, z, velocityX, velocityY, velocityZ, yaw, pitch, roll, destroyed));
        }
        contacts = List.copyOf(decoded);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        DistantContactsClient.accept(this);
    }
}
