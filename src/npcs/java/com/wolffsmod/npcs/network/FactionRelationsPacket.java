package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.names.FactionDisplayName;
import com.wolffsmod.npcs.teams.FactionAllies;
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
 * The Team factions page's view of the Custom NPCs factions and how they stand toward each other. The client asks for the
 * list, or for the chosen factions to be set to a relation; the server answers with the current list either way.
 */
public record FactionRelationsPacket(int entityId, @Nullable FactionRelations.Relation relation, List<Integer> chosen, List<Entry> factions)
{
    /** A faction, with the ids of the listed factions it attacks and is allied with. */
    public record Entry(int id, Component name, int colour, List<Integer> enemies, List<Integer> allies)
    {
        void write(FriendlyByteBuf buffer)
        {
            buffer.writeVarInt(id);
            buffer.writeComponent(name);
            buffer.writeInt(colour);
            buffer.writeCollection(enemies, FriendlyByteBuf::writeVarInt);
            buffer.writeCollection(allies, FriendlyByteBuf::writeVarInt);
        }

        static Entry read(FriendlyByteBuf buffer)
        {
            return new Entry(buffer.readVarInt(), buffer.readComponent(), buffer.readInt(), buffer.readList(FriendlyByteBuf::readVarInt), buffer.readList(FriendlyByteBuf::readVarInt));
        }
    }

    public static FactionRelationsPacket list(int entityId)
    {
        return new FactionRelationsPacket(entityId, null, List.of(), List.of());
    }

    public static FactionRelationsPacket set(int entityId, FactionRelations.Relation relation, Collection<Integer> chosen)
    {
        return new FactionRelationsPacket(entityId, relation, List.copyOf(chosen), List.of());
    }

    /** @return the server's answer listing every editable faction */
    public static FactionRelationsPacket state(int entityId)
    {
        List<Faction> editable = FactionRelations.editable();
        Set<Integer> ids = new HashSet<>();
        editable.forEach(faction -> ids.add(faction.id));
        List<Entry> entries = editable.stream()
            .map(faction -> new Entry(faction.id, FactionDisplayName.of(faction), faction.color & 0xFFFFFF, listed(faction.attackFactions, ids), listed(FactionAllies.of(faction), ids))).toList();
        return new FactionRelationsPacket(entityId, null, List.of(), entries);
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
    }

    public static FactionRelationsPacket read(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        FactionRelations.Relation relation = buffer.readBoolean() ? buffer.readEnum(FactionRelations.Relation.class) : null;
        List<Integer> chosen = buffer.readList(FriendlyByteBuf::readVarInt);
        List<Entry> factions = buffer.readList(Entry::read);
        return new FactionRelationsPacket(entityId, relation, chosen, factions);
    }

    /** Lists the factions, after setting the chosen ones' relation, for a builder holding the wand while editing this NPC. */
    public void handle(ServerPlayer player)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        boolean allowed = npc != null && npc.getId() == entityId && npc.isAlive() && npc.level() == player.level() && player.getInventory().getSelected().getItem() == CustomItems.wand
            && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_FACTION);
        if (!allowed)
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
        NpcNetwork.sendTo(player, state(entityId));
    }
}
