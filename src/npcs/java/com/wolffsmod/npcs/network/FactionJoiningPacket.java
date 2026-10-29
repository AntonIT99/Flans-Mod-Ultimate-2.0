package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.names.FactionDisplayName;
import com.wolffsmod.npcs.teams.BotFactions;
import com.wolffsmod.npcs.teams.FactionJoining;
import com.wolffsmod.npcs.teams.FactionMembers;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A builder's change, on the Team factions page, to who may join a faction and what leaving it costs, as
 * {@code /teams faction joining} makes it, or to their own membership, as {@code /teams faction member} makes it: whatever
 * the joining rules and without a leave penalty. The server answers with the faction list.
 *
 * @param penalty
 *            the faction points a player loses on leaving the faction
 * @param cooldown
 *            the minutes before a player who left may join again
 */
public record FactionJoiningPacket(int entityId, int faction, FactionJoining.Rule rule, int penalty, int cooldown, Membership membership)
{
    /** What becomes of the builder's own membership. */
    public enum Membership
    {
        KEEP, JOIN, LEAVE
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(faction);
        buffer.writeEnum(rule);
        buffer.writeVarInt(penalty);
        buffer.writeVarInt(cooldown);
        buffer.writeEnum(membership);
    }

    public static FactionJoiningPacket read(FriendlyByteBuf buffer)
    {
        return new FactionJoiningPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readEnum(FactionJoining.Rule.class), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readEnum(Membership.class));
    }

    /** Applies the change for a builder holding the wand while editing this NPC; factions of Teams rounds are left alone. */
    public void handle(ServerPlayer player)
    {
        if (!FactionRelationsPacket.allowed(player, entityId))
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.team_faction.denied"));
            return;
        }
        Faction found = FactionController.instance.getFaction(faction);
        if (found != null && !BotFactions.isRoundFaction(found.name))
        {
            FactionJoining joining = FactionJoining.of(found);
            if (joining.wolffsmodnpcsJoinRule() != rule || joining.wolffsmodnpcsLeavePenalty() != penalty || joining.wolffsmodnpcsRejoinCooldown() != cooldown)
            {
                joining.wolffsmodnpcsSetJoinRule(rule);
                joining.wolffsmodnpcsSetLeavePenalty(penalty);
                joining.wolffsmodnpcsSetRejoinCooldown(cooldown);
                FactionController.instance.saveFaction(found);
            }
            membership(player, found);
        }
        NpcNetwork.sendTo(player, FactionRelationsPacket.state(entityId, player));
    }

    private void membership(ServerPlayer player, Faction found)
    {
        FactionMembers members = FactionMembers.get(player.server);
        boolean member = members.factionOf(player.getUUID()).filter(id -> id == found.id).isPresent();
        if (membership == Membership.JOIN && !member)
        {
            members.setMember(player.getUUID(), found.id);
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.command.faction.joined", FactionDisplayName.of(found)));
        }
        else if (membership == Membership.LEAVE && member)
        {
            members.setMember(player.getUUID(), null);
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.command.faction.left", FactionDisplayName.of(found)));
        }
    }
}
