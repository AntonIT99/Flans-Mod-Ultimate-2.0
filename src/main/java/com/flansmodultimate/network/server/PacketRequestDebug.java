package com.flansmodultimate.network.server;

import com.flansmodultimate.common.driveables.DriveableDamageDebug;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketAllowDebug;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketRequestDebug implements IServerPacket
{
    private boolean enabled = true;

    public PacketRequestDebug(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(enabled);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        enabled = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        boolean allowed = enabled && player.hasPermissions(2);
        DriveableDamageDebug.setEnabled(player, allowed);
        if (allowed)
            PacketHandler.sendTo(new PacketAllowDebug(), player);
    }
}
