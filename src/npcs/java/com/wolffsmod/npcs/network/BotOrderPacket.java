package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.teams.BotOrders;
import com.wolffsmod.npcs.teams.PlayerOrder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * A player's order to squads of bots, or a request for their order screen. The server checks that the player may give
 * the order, carries it out and sends the screen's state back.
 *
 * @param action
 *            what the player asks for
 * @param squadId
 *            the squad ordered, or {@link BotOrders#NEARBY_SQUADS}
 * @param order
 *            the order, for {@link Action#ORDER}
 * @param objective
 *            the capture point an order to attack or defend names, by play order, or {@link BotOrders#HERE}
 */
public record BotOrderPacket(Action action, int squadId, PlayerOrder.Kind order, int objective)
{
    /** What a player asks of the server. */
    public enum Action
    {
        /** Only the state of the order screen. */
        REFRESH, ORDER,
        /** Taking the command seat of their side. */
        TAKE_COMMAND,
        /** Leaving it. */
        LEAVE_COMMAND
    }

    public static BotOrderPacket refresh()
    {
        return new BotOrderPacket(Action.REFRESH, BotOrders.NEARBY_SQUADS, PlayerOrder.Kind.RELEASE, BotOrders.HERE);
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeEnum(action);
        buffer.writeVarInt(squadId);
        buffer.writeEnum(order);
        buffer.writeVarInt(objective);
    }

    public static BotOrderPacket read(FriendlyByteBuf buffer)
    {
        return new BotOrderPacket(buffer.readEnum(Action.class), buffer.readVarInt(), buffer.readEnum(PlayerOrder.Kind.class), buffer.readVarInt());
    }

    public void handle(ServerPlayer player)
    {
        switch (action)
        {
            case ORDER -> BotOrders.give(player, squadId, order, objective);
            case TAKE_COMMAND -> BotOrders.command(player, true);
            case LEAVE_COMMAND -> BotOrders.command(player, false);
            case REFRESH -> {
                // Only the state is sent back.
            }
        }
        NpcNetwork.sendTo(player, BotOrders.state(player));
    }
}
