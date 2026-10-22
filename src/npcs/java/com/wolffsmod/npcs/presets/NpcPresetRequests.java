package com.wolffsmod.npcs.presets;

import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.NpcPresetPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** A builder's requests from the presets of the Wolff's Mod tab, checked against what Custom NPCs lets the builder do. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPresetRequests
{
    /**
     * Sends the player the preset settings of the NPC they are editing. The tab only receives the statistics with Custom
     * NPCs' own packets, and needs the AI settings as the server has them to show what a preset changes.
     */
    public static void sendState(ServerPlayer player, int entityId)
    {
        editing(player, entityId).ifPresent(npc -> NpcNetwork.sendTo(player, NpcPresetPacket.state(npc)));
    }

    /**
     * Gives the NPC the player is editing every setting of the preset, then sends them the settings as they now are. The
     * player needs the wand and the permissions Custom NPCs asks for to change an NPC's AI and statistics, since a preset
     * sets both.
     */
    public static void apply(ServerPlayer player, int entityId, NpcPreset preset)
    {
        Optional<EntityNPCInterface> npc = editing(player, entityId).filter(editing -> player.getInventory().getSelected().getItem() == CustomItems.wand
            && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_AI) && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_STATS));
        if (npc.isEmpty())
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.preset.denied"));
            return;
        }
        preset.apply(npc.get());
        NpcNetwork.sendTo(player, NpcPresetPacket.state(npc.get()));
        player.sendSystemMessage(Component.translatable("wolffsmodnpcs.preset.applied", Component.translatable(preset.key())));
    }

    /** @return the NPC the request was made for, when it is the one the player is editing, which Custom NPCs only records for a player allowed to open its editor */
    private static Optional<EntityNPCInterface> editing(ServerPlayer player, int entityId)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        return npc != null && npc.getId() == entityId && npc.isAlive() && npc.level() == player.level() ? Optional.of(npc) : Optional.empty();
    }
}
