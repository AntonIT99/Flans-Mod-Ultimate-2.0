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

import java.util.*;

/** A builder's requests from the presets of the Wolff's Mod tab, checked against what Custom NPCs lets the builder do. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPresetRequests
{
    private static final Map<EntityNPCInterface, Map<NpcPresetSetting, Integer>> UNDO = new WeakHashMap<>();

    public static boolean canUndo(EntityNPCInterface npc)
    {
        return UNDO.containsKey(npc);
    }

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
        Optional<EntityNPCInterface> npc = permitted(player, entityId);
        if (npc.isEmpty())
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.preset.denied"));
            return;
        }
        apply(npc.get(), preset);
        NpcNetwork.sendTo(player, NpcPresetPacket.state(npc.get()));
        player.sendSystemMessage(Component.translatable("wolffsmodnpcs.preset.applied", Component.translatable(preset.key())));
    }

    static void apply(EntityNPCInterface npc, NpcPreset preset)
    {
        if (!preset.matches(npc))
        {
            Map<NpcPresetSetting, Integer> previous = new EnumMap<>(NpcPresetSetting.class);
            for (NpcPreset.Target target : preset.getTargets())
                if (target.setting().get(npc) != preset.value(npc, target))
                    previous.put(target.setting(), target.setting().get(npc));
            preset.apply(npc);
            UNDO.put(npc, previous);
        }
    }

    static void undo(EntityNPCInterface npc)
    {
        Map<NpcPresetSetting, Integer> previous = UNDO.remove(npc);
        if (previous != null)
        {
            previous.forEach((setting, value) -> setting.set(npc, value));
            npc.updateAI = true;
            npc.updateClient = true;
        }
    }

    /** Restores only settings changed by the last application, with the same authorization as Apply. */
    public static void undo(ServerPlayer player, int entityId)
    {
        Optional<EntityNPCInterface> npc = permitted(player, entityId);
        if (npc.isEmpty())
        {
            player.sendSystemMessage(Component.translatable("wolffsmodnpcs.preset.denied"));
            return;
        }
        undo(npc.get());
        NpcNetwork.sendTo(player, NpcPresetPacket.state(npc.get()));
    }

    private static Optional<EntityNPCInterface> permitted(ServerPlayer player, int entityId)
    {
        return editing(player, entityId).filter(npc -> player.getInventory().getSelected().getItem() == CustomItems.wand && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_AI)
            && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_STATS));
    }

    /** @return the NPC the request was made for, when it is the one the player is editing, which Custom NPCs only records for a player allowed to open its editor */
    private static Optional<EntityNPCInterface> editing(ServerPlayer player, int entityId)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        return npc != null && npc.getId() == entityId && npc.isAlive() && npc.level() == player.level() ? Optional.of(npc) : Optional.empty();
    }
}
