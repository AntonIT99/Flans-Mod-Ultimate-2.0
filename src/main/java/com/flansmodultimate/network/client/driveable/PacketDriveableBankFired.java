package com.flansmodultimate.network.client.driveable;

import com.flansmodultimate.client.render.entity.DriveableMuzzleFlashes;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A driveable weapon bank fired from some of its shoot points. Clients place both flashes and
 * authored particles from their own view of the driveable, so a shot costs one small packet
 * rather than one per effect. A point on a twin or quad mount also names the fired barrel.
 */
@NoArgsConstructor
public class PacketDriveableBankFired implements IClientPacket
{
    /** More shoot points than any bank has; guards the decoder against a corrupt count. */
    private static final int MAX_POINTS = 256;

    private int entityId;
    private boolean secondary;
    private int[] pointIndices = new int[0];
    /** The barrel of each point in {@link #pointIndices} that fired, index for index. */
    private int[] barrels = new int[0];

    public PacketDriveableBankFired(int entityId, boolean secondary, int[] pointIndices, int[] barrels)
    {
        this.entityId = entityId;
        this.secondary = secondary;
        this.pointIndices = pointIndices;
        this.barrels = barrels;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeBoolean(secondary);
        data.writeVarInt(pointIndices.length);
        for (int index = 0; index < pointIndices.length; index++)
        {
            data.writeVarInt(pointIndices[index]);
            data.writeVarInt(index < barrels.length ? barrels[index] : 0);
        }
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        secondary = data.readBoolean();
        int count = Math.min(data.readVarInt(), MAX_POINTS);
        pointIndices = new int[count];
        barrels = new int[count];
        for (int i = 0; i < count; i++)
        {
            pointIndices[i] = data.readVarInt();
            barrels[i] = data.readVarInt();
        }
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(entityId) instanceof Driveable driveable)
        {
            DriveableMuzzleFlashes.bankFired(driveable, secondary, pointIndices, barrels);
            driveable.spawnBankParticles(secondary, pointIndices, barrels);
        }
    }
}
