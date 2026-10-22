package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.teams.BotLeaders;
import com.wolffsmod.npcs.teams.BotOrders;
import com.wolffsmod.npcs.teams.PlayerOrder;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

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
 * @param at
 *            a place picked on the commander map, across the ground, or null
 */
public record BotOrderPacket(Action action, int squadId, PlayerOrder.Kind order, int objective, @Nullable Vec3 at)
{
    /** What a player asks of the server. */
    public enum Action
    {
        /** The order screen, opening it. */
        OPEN,
        /** Only the state of an order screen already open. */
        REFRESH, ORDER,
        /** Taking the command seat of their side. */
        TAKE_COMMAND,
        /** Leaving it. */
        LEAVE_COMMAND,
        /** Leading the squad chosen, or the nearest one. */
        LEAD,
        /** Giving up the squad the player leads. */
        LEAVE_SQUAD,
        /** Placing the rally point of the squad the player leads. */
        RALLY,
        /** Choosing where to come back after dying, as the objective field gives it. */
        SPAWN_CHOICE
    }

    public static BotOrderPacket refresh()
    {
        return of(Action.REFRESH);
    }

    /** An action that names no squad, order or place. */
    public static BotOrderPacket of(Action action)
    {
        return new BotOrderPacket(action, BotOrders.NEARBY_SQUADS, PlayerOrder.Kind.RELEASE, BotOrders.HERE, null);
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeEnum(action);
        buffer.writeVarInt(squadId);
        buffer.writeEnum(order);
        buffer.writeVarInt(objective);
        buffer.writeBoolean(at != null);
        if (at != null)
        {
            buffer.writeDouble(at.x);
            buffer.writeDouble(at.z);
        }
    }

    public static BotOrderPacket read(FriendlyByteBuf buffer)
    {
        Action action = buffer.readEnum(Action.class);
        int squadId = buffer.readVarInt();
        PlayerOrder.Kind order = buffer.readEnum(PlayerOrder.Kind.class);
        int objective = buffer.readVarInt();
        Vec3 at = buffer.readBoolean() ? new Vec3(buffer.readDouble(), 0D, buffer.readDouble()) : null;
        return new BotOrderPacket(action, squadId, order, objective, at);
    }

    public void handle(ServerPlayer player)
    {
        switch (action)
        {
            case ORDER -> BotOrders.give(player, squadId, order, objective, at != null && Double.isFinite(at.x) && Double.isFinite(at.z) ? at : null);
            case TAKE_COMMAND -> BotOrders.command(player, true);
            case LEAVE_COMMAND -> BotOrders.command(player, false);
            case LEAD -> player.sendSystemMessage(BotLeaders.lead(player, squadId));
            case LEAVE_SQUAD -> player.sendSystemMessage(BotLeaders.leave(player));
            case RALLY -> player.sendSystemMessage(BotLeaders.rally(player));
            case SPAWN_CHOICE -> {
                BotLeaders.SpawnChoice[] choices = BotLeaders.SpawnChoice.values();
                player.sendSystemMessage(BotLeaders.chooseSpawn(player, choices[Math.floorMod(objective, choices.length)]));
            }
            case OPEN, REFRESH -> {
                // Only the state is sent back.
            }
        }
        NpcNetwork.sendTo(player, BotOrders.state(player).opening(action == Action.OPEN));
    }
}
