package com.flansmodultimate.network.client;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A driveable weapon bank fired from some of its shoot points. Clients take the particles from
 * the driveable's type and place them on their own view of it, so a shot costs one small packet
 * rather than one per particle per shoot point.
 */
@NoArgsConstructor
public class PacketDriveableBankFired implements IClientPacket
{
    /** More shoot points than any bank has; guards the decoder against a corrupt count. */
    private static final int MAX_POINTS = 256;

    private int entityId;
    private boolean secondary;
    private int[] pointIndices = new int[0];

    public PacketDriveableBankFired(int entityId, boolean secondary, int[] pointIndices)
    {
        this.entityId = entityId;
        this.secondary = secondary;
        this.pointIndices = pointIndices;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeBoolean(secondary);
        data.writeVarInt(pointIndices.length);
        for (int index : pointIndices)
            data.writeVarInt(index);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        secondary = data.readBoolean();
        int count = Math.min(data.readVarInt(), MAX_POINTS);
        pointIndices = new int[count];
        for (int i = 0; i < count; i++)
            pointIndices[i] = data.readVarInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(entityId) instanceof Driveable driveable)
            driveable.spawnBankParticles(secondary, pointIndices);
    }
}
