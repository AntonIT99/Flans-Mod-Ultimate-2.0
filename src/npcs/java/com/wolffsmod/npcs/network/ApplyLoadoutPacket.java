package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.soldier.FlanSoldierLoadout;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * The model picker's "Apply class loadout" button: a client asks the server to equip the NPC it is editing with the
 * loadout of a Flan's player class. The names travel with the request because the picker only saves the model, and with
 * it the selection, when it is closed.
 *
 * @param entityId
 *            the NPC being edited, so a request that arrives for another NPC is refused
 * @param team
 *            short name of the team
 * @param playerClass
 *            short name of the class
 */
public record ApplyLoadoutPacket(int entityId, String team, String playerClass)
{
    /** Longest short name accepted; content-pack short names are far shorter. */
    private static final int MAX_NAME_LENGTH = 256;

    public static ApplyLoadoutPacket of(int entityId, FlanSoldierSelection selection)
    {
        return new ApplyLoadoutPacket(entityId, selection.team() == null ? "" : selection.team().getShortName(), selection.playerClass().getShortName());
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeUtf(team, MAX_NAME_LENGTH);
        buffer.writeUtf(playerClass, MAX_NAME_LENGTH);
    }

    public static ApplyLoadoutPacket read(FriendlyByteBuf buffer)
    {
        return new ApplyLoadoutPacket(buffer.readVarInt(), buffer.readUtf(MAX_NAME_LENGTH), buffer.readUtf(MAX_NAME_LENGTH));
    }

    /** Checks that the player may edit the NPC, then equips it. */
    public void handle(ServerPlayer player)
    {
        FlanSoldierLoadout.request(player, entityId, team, playerClass);
    }
}
