package com.flansmodultimate.network.client;

import com.flansmodultimate.client.render.KillMessageData;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Adds one entry to the client kill feed. */
@NoArgsConstructor
public class PacketKillMessage implements IClientPacket
{
    private KillMessageData message =
        new KillMessageData(false, "", "", ChatFormatting.WHITE, "", ChatFormatting.WHITE);

    public PacketKillMessage(KillMessageData message)
    {
        this.message = message;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(message.headshot());
        data.writeUtf(message.weaponShortName());
        data.writeUtf(message.killerName());
        data.writeEnum(message.killerColour());
        data.writeUtf(message.victimName());
        data.writeEnum(message.victimColour());
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        message = new KillMessageData(data.readBoolean(), data.readUtf(),
            data.readUtf(), data.readEnum(ChatFormatting.class),
            data.readUtf(), data.readEnum(ChatFormatting.class));
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.addKillMessage(message);
    }
}
