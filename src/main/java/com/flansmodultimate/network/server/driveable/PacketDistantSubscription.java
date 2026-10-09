package com.flansmodultimate.network.server.driveable;

import com.flansmodultimate.common.distant.DistantSync;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tells the server what a client that draws far terrain wants to see there: distant driveables, large
 * distant explosions, or neither. Sent on login and whenever the client's settings change. Only decides what
 * display data the player is sent; the server's own settings still decide whether any is sent at all.
 */
@NoArgsConstructor
public class PacketDistantSubscription implements IServerPacket
{
    private boolean contacts;
    private boolean explosions;

    public PacketDistantSubscription(boolean contacts, boolean explosions)
    {
        this.contacts = contacts;
        this.explosions = explosions;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(contacts);
        data.writeBoolean(explosions);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        contacts = data.readBoolean();
        explosions = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        DistantSync.subscribe(player, contacts, explosions);
    }
}
