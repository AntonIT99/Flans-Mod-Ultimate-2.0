package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.client.gui.TeamsRankUpPopup;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Tells a player they were promoted, which slides the rank-up notice in at the top of the screen. */
@NoArgsConstructor
public final class PacketRankUp implements IClientPacket
{
    private int oldRank;
    private int newRank;
    private int prestige;

    public PacketRankUp(int oldRank, int newRank, int prestige)
    {
        this.oldRank = oldRank;
        this.newRank = newRank;
        this.prestige = prestige;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(oldRank);
        data.writeVarInt(newRank);
        data.writeVarInt(prestige);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        oldRank = data.readVarInt();
        newRank = data.readVarInt();
        prestige = data.readVarInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        TeamsRankUpPopup.show(oldRank, newRank, prestige);
    }
}
