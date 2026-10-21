package com.wolffsmod.npcs.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * What a player's squad order screen shows: the squads they may order, the objectives of the round, who holds their
 * side's command seat, and the squad they lead. Sent by the server when the player opens the screen and after each order.
 *
 * @param playing
 *            whether the player fights in a round with bots; nothing else is filled in otherwise
 * @param commander
 *            whether the player holds the command seat of their side
 * @param commanderName
 *            who holds it, empty while nobody does
 * @param open
 *            whether the screen opens for it; a reply to a refresh only updates a screen that is still open
 */
public record BotOrdersStatePacket(boolean playing, int side, boolean commander, String commanderName, List<Squad> squads, List<Objective> objectives, Leading leading, boolean open)
{
    private static final int MAX_TEXT = 128;

    /**
     * A squad the player may order.
     *
     * @param mine
     *            whether it follows an order of this player
     * @param distance
     *            blocks between the player and its leader
     * @param x
     *            where its leader is, across the ground
     */
    public record Squad(int id, String name, int strength, String task, boolean mine, int distance, double x, double z)
    {}

    /**
     * A capture point of the round.
     *
     * @param ownerSide
     *            the side that holds it, or -1
     * @param x
     *            where it is, across the ground
     */
    public record Objective(char symbol, String name, int ownerSide, double x, double z)
    {}

    /**
     * The squad the player leads.
     *
     * @param squad
     *            its id, or -1 while the player leads none
     * @param rally
     *            whether it has a rally point
     * @param spawnChoice
     *            where the player comes back after dying, by the order of the server's choices
     * @param rallyPoints
     *            whether the round allows rally points
     */
    public record Leading(int squad, boolean rally, int spawnChoice, boolean rallyPoints)
    {
        public static final Leading NONE = new Leading(-1, false, 0, false);
    }

    public BotOrdersStatePacket
    {
        squads = List.copyOf(squads);
        objectives = List.copyOf(objectives);
    }

    public static BotOrdersStatePacket notPlaying()
    {
        return new BotOrdersStatePacket(false, -1, false, "", List.of(), List.of(), Leading.NONE, false);
    }

    /** The same state, opening the screen or only updating an open one. */
    public BotOrdersStatePacket opening(boolean opens)
    {
        return new BotOrdersStatePacket(playing, side, commander, commanderName, squads, objectives, leading, opens);
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(playing);
        buffer.writeVarInt(side);
        buffer.writeBoolean(commander);
        buffer.writeUtf(commanderName, MAX_TEXT);
        buffer.writeCollection(squads, (out, squad) ->
        {
            out.writeVarInt(squad.id());
            out.writeUtf(squad.name(), MAX_TEXT);
            out.writeVarInt(squad.strength());
            out.writeUtf(squad.task(), MAX_TEXT);
            out.writeBoolean(squad.mine());
            out.writeVarInt(squad.distance());
            out.writeDouble(squad.x());
            out.writeDouble(squad.z());
        });
        buffer.writeCollection(objectives, (out, objective) ->
        {
            out.writeChar(objective.symbol());
            out.writeUtf(objective.name(), MAX_TEXT);
            out.writeVarInt(objective.ownerSide());
            out.writeDouble(objective.x());
            out.writeDouble(objective.z());
        });
        buffer.writeVarInt(leading.squad());
        buffer.writeBoolean(leading.rally());
        buffer.writeVarInt(leading.spawnChoice());
        buffer.writeBoolean(leading.rallyPoints());
        buffer.writeBoolean(open);
    }

    public static BotOrdersStatePacket read(FriendlyByteBuf buffer)
    {
        boolean playing = buffer.readBoolean();
        int side = buffer.readVarInt();
        boolean commander = buffer.readBoolean();
        String commanderName = buffer.readUtf(MAX_TEXT);
        List<Squad> squads = buffer
            .readList(in -> new Squad(in.readVarInt(), in.readUtf(MAX_TEXT), in.readVarInt(), in.readUtf(MAX_TEXT), in.readBoolean(), in.readVarInt(), in.readDouble(), in.readDouble()));
        List<Objective> objectives = buffer.readList(in -> new Objective(in.readChar(), in.readUtf(MAX_TEXT), in.readVarInt(), in.readDouble(), in.readDouble()));
        Leading leading = new Leading(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean());
        boolean open = buffer.readBoolean();
        return new BotOrdersStatePacket(playing, side, commander, commanderName, squads, objectives, leading, open);
    }
}
