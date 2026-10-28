package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.names.FactionDisplayName;
import com.wolffsmod.npcs.teams.FactionAllies;
import com.wolffsmod.npcs.teams.FactionJoining;
import com.wolffsmod.npcs.teams.FactionMembers;
import com.wolffsmod.npcs.teams.FactionRelations;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * The Team factions page's view of the Custom NPCs factions, how they stand toward each other and who may join them. The
 * client asks for the list, or for the chosen factions to be set to a relation; the server answers with the current list
 * either way, and after every change {@link FactionJoiningPacket} makes.
 *
 * @param own
 *            the faction the builder belongs to, in the server's answer, or {@link #NO_FACTION}
 */
public record FactionRelationsPacket(int entityId, @Nullable FactionRelations.Relation relation, List<Integer> chosen, List<Entry> factions, int own)
{
    public static final int NO_FACTION = -1;

    /** A faction, with the ids of the listed factions it attacks and is allied with, its joining rules and its member count. */
    public record Entry(int id, Component name, int colour, List<Integer> enemies, List<Integer> allies, FactionJoining.Rule rule, int penalty, int cooldown, int members)
    {
        void write(FriendlyByteBuf buffer)
        {
            buffer.writeVarInt(id);
            buffer.writeComponent(name);
            buffer.writeInt(colour);
            buffer.writeCollection(enemies, FriendlyByteBuf::writeVarInt);
            buffer.writeCollection(allies, FriendlyByteBuf::writeVarInt);
            buffer.writeEnum(rule);
            buffer.writeVarInt(penalty);
            buffer.writeVarInt(cooldown);
            buffer.writeVarInt(members);
        }

        static Entry read(FriendlyByteBuf buffer)
        {
            return new Entry(buffer.readVarInt(), buffer.readComponent(), buffer.readInt(), buffer.readList(FriendlyByteBuf::readVarInt), buffer.readList(FriendlyByteBuf::readVarInt),
                buffer.readEnum(FactionJoining.Rule.class), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }
    }

    public static FactionRelationsPacket list(int entityId)
    {
        return new FactionRelationsPacket(entityId, null, List.of(), List.of(), NO_FACTION);
    }

    public static FactionRelationsPacket set(int entityId, FactionRelations.Relation relation, Collection<Integer> chosen)
    {
        return new FactionRelationsPacket(entityId, relation, List.copyOf(chosen), List.of(), NO_FACTION);
    }

    /** @return the server's answer listing every editable faction, for the builder viewing them */
    public static FactionRelationsPacket state(int entityId, ServerPlayer player)
    {
        FactionMembers members = FactionMembers.get(player.server);
        List<Faction> editable = FactionRelations.editable();
        Set<Integer> ids = new HashSet<>();
        editable.forEach(faction -> ids.add(faction.id));
        List<Entry> entries = editable.stream().map(faction -> entry(faction, ids, (int) members.memberCount(faction.id))).toList();
        int own = members.factionOf(player.getUUID()).filter(ids::contains).orElse(NO_FACTION);
        return new FactionRelationsPacket(entityId, null, List.of(), entries, own);
    }

    private static Entry entry(Faction faction, Set<Integer> ids, int members)
    {
        FactionJoining joining = FactionJoining.of(faction);
        return new Entry(faction.id, FactionDisplayName.of(faction), faction.color & 0xFFFFFF, listed(faction.attackFactions, ids), listed(FactionAllies.of(faction), ids),
            joining.wolffsmodnpcsJoinRule(), joining.wolffsmodnpcsLeavePenalty(), joining.wolffsmodnpcsRejoinCooldown(), members);
    }

    private static List<Integer> listed(Collection<Integer> factions, Set<Integer> ids)
    {
        return factions.stream().filter(ids::contains).sorted().toList();
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(relation != null);
        if (relation != null)
            buffer.writeEnum(relation);
        buffer.writeCollection(chosen, FriendlyByteBuf::writeVarInt);
        buffer.writeCollection(factions, (out, entry) -> entry.write(out));
        buffer.writeVarInt(own);
    }

    public static FactionRelationsPacket read(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        FactionRelations.Relation relation = buffer.readBoolean() ? buffer.readEnum(FactionRelations.Relation.class) : null;
        List<Integer> chosen = buffer.readList(FriendlyByteBuf::readVarInt);
        List<Entry> factions = buffer.readList(Entry::read);
        return new FactionRelationsPacket(entityId, relation, chosen, factions, buffer.readVarInt());
    }

    /** Whether the player is a builder holding the wand while editing this NPC, with leave to edit factions. */
    static boolean allowed(ServerPlayer player, int entityId)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        return npc != null && npc.getId() == entityId && npc.isAlive() && npc.level() == player.level() && player.getInventory().getSelected().getItem() == CustomItems.wand
            && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_FACTION);
    }

    /** Lists the factions, after setting the chosen ones' relation, for a builder holding the wand while editing this NPC. */
    public void handle(ServerPlayer player)
    {
        if (!allowed(player, entityId))
        {
            if (relation != null)
                player.sendSystemMessage(Component.translatable("wolffsmodnpcs.team_faction.denied"));
            return;
        }
        if (relation != null)
        {
            List<Faction> picked = chosen.stream().distinct().map(FactionController.instance::getFaction).filter(Objects::nonNull).toList();
            if (picked.size() >= 2)
            {
                FactionRelations.set(picked, relation);
                player.sendSystemMessage(Component.translatable("wolffsmodnpcs.team_faction.relation." + relation.name().toLowerCase(Locale.ROOT), picked.size()));
            }
        }
        NpcNetwork.sendTo(player, state(entityId, player));
    }
}
