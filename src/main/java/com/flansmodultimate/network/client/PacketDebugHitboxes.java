package com.flansmodultimate.network.client;

import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.Map;

/** Complete, idempotent server-authoritative runtime geometry snapshot. */
public final class PacketDebugHitboxes implements IClientPacket
{
    private static final java.util.Set<DriveableType> EDITED = new java.util.HashSet<>();

    public static void clearSession()
    {
        EDITED.forEach(DriveableType::resetDebugHitboxes);
        EDITED.clear();
    }

    private String shortName = "";
    private boolean reset;
    private final Map<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
    public PacketDebugHitboxes() {}
    public PacketDebugHitboxes(DriveableType type, boolean reset)
    {
        shortName = type.getShortName();
        this.reset = reset;
        if (!reset)
            boxes.putAll(type.getHealth());
    }
    @Override public void encodeInto(PacketBuffer data)
    {
        data.writeUtf(shortName);
        data.writeBoolean(reset);
        data.writeVarInt(boxes.size());
        boxes.forEach((part, box) -> {
            data.writeUtf(part.getShortName());
            for (float value : new float[] {box.getHealth(), box.getX(), box.getY(), box.getZ(),
                box.getWidth(), box.getHeight(), box.getDepth(), box.getPenetrationResistance(), box.getCrewDamageMultiplier()})
                data.writeFloat(value);
        });
    }
    @Override public void decodeInto(PacketBuffer data)
    {
        shortName = data.readUtf();
        reset = data.readBoolean();
        int count = data.readVarInt();
        if (count < 0 || count > EnumDriveablePart.values().length)
            throw new IllegalArgumentException("Invalid hitbox count");
        boxes.clear();
        for (int i = 0; i < count; i++)
        {
            EnumDriveablePart part = EnumDriveablePart.getPart(data.readUtf());
            float[] v = new float[9];
            for (int j = 0; j < v.length; j++)
            {
                v[j] = data.readFloat();
                if (!Float.isFinite(v[j]))
                    throw new IllegalArgumentException("Non-finite hitbox value");
            }
            if (part == null || boxes.containsKey(part) || v[0] < 0 || v[4] < 0 || v[5] < 0 || v[6] < 0)
                throw new IllegalArgumentException("Invalid hitbox");
            boxes.put(part, CollisionBox.inWorldUnits(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]));
        }
    }
    @Override public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (InfoType.getInfoType(shortName) instanceof DriveableType type)
        {
            if (reset)
            {
                type.resetDebugHitboxes();
                EDITED.remove(type);
            }
            else
            {
                type.setDebugHitboxes(boxes);
                EDITED.add(type);
            }
        }
    }
}
