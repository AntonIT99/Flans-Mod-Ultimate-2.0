package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A vehicle smoke-launcher shell being fired. Clients fly it, trail it and burst it themselves,
 * so the server sends this once instead of a particle packet every few ticks of its flight.
 */
@NoArgsConstructor
public class PacketSmokeShell implements IClientPacket
{
    /** Longest fuse a smoke point may configure: one minute. */
    private static final int MAX_FUSE_TICKS = 20 * 60;

    private float x;
    private float y;
    private float z;
    private float vx;
    private float vy;
    private float vz;
    private int fuseTicks;

    public PacketSmokeShell(Vec3 origin, Vec3 velocity, int fuseTicks)
    {
        x = (float) origin.x;
        y = (float) origin.y;
        z = (float) origin.z;
        vx = (float) velocity.x;
        vy = (float) velocity.y;
        vz = (float) velocity.z;
        this.fuseTicks = fuseTicks;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeFloat(z);
        data.writeFloat(vx);
        data.writeFloat(vy);
        data.writeFloat(vz);
        data.writeVarInt(fuseTicks);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        x = data.readFloat();
        y = data.readFloat();
        z = data.readFloat();
        vx = data.readFloat();
        vy = data.readFloat();
        vz = data.readFloat();
        fuseTicks = data.readVarInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (fuseTicks < 1 || fuseTicks > MAX_FUSE_TICKS)
            return;
        ClientHooks.RENDER.launchSmokeShell(x, y, z, vx, vy, vz, fuseTicks);
    }
}
