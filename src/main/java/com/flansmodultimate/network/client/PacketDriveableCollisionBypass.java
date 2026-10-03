package com.flansmodultimate.network.client;

import com.flansmodultimate.common.driveables.DriveableCollisionBypass;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Synchronizes a temporary driveable-hull collision bypass to a client: the receiver's own, or the all-entities one. */
public final class PacketDriveableCollisionBypass implements IClientPacket
{
    private boolean allEntities;
    private boolean enabled;

    public PacketDriveableCollisionBypass()
    {
    }

    public PacketDriveableCollisionBypass(boolean allEntities, boolean enabled)
    {
        this.allEntities = allEntities;
        this.enabled = enabled;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(allEntities);
        data.writeBoolean(enabled);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        allEntities = data.readBoolean();
        enabled = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (allEntities)
            DriveableCollisionBypass.setAllEntities(enabled);
        else
            DriveableCollisionBypass.setEnabled(player, enabled);
    }
}
