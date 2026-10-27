package com.wolffsmod.npcs.network;

import com.flansmodultimate.api.FlansModApi;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import com.wolffsmod.npcs.teams.TeamFactions;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Explicit requests to create a team's faction or apply it to the NPC being edited. */
public record TeamFactionPacket(int entityId, String team, String playerClass, boolean apply)
{
    private static final int MAX_LENGTH = 256;

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeUtf(team, MAX_LENGTH);
        buffer.writeUtf(playerClass, MAX_LENGTH);
        buffer.writeBoolean(apply);
    }

    public static TeamFactionPacket read(FriendlyByteBuf buffer)
    {
        return new TeamFactionPacket(buffer.readVarInt(), buffer.readUtf(MAX_LENGTH), buffer.readUtf(MAX_LENGTH), buffer.readBoolean());
    }

    public void handle(ServerPlayer player)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        boolean allowed = npc != null && npc.getId() == entityId && npc.isAlive() && npc.level() == player.level() && player.getInventory().getSelected().getItem() == CustomItems.wand
            && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_FACTION) && (!apply || CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_ADVANCED));
        if (!allowed)
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.team_faction.denied"));
            return;
        }
        var selected = apply ? FlanSoldierSelection.resolve(team, playerClass).map(FlanSoldierSelection::team) : FlansModApi.getTeam(team);
        if (selected.isEmpty())
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.team_faction.unknown"));
            return;
        }
        TeamFactions factions = TeamFactions.get(player.server);
        if (apply)
            factions.apply(npc, selected.get());
        else
            factions.getOrCreate(selected.get());
        player.sendSystemMessage(Component.translatable("wolffsmodnpcs.team_faction." + (apply ? "applied" : "created"), selected.get().getNameComponent()));
        // The Team factions page lists the new faction.
        if (!apply)
            NpcNetwork.sendTo(player, FactionRelationsPacket.state(entityId));
    }
}
