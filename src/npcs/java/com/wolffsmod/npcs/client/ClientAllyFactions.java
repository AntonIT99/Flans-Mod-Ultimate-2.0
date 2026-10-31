package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.AllyFactionsPacket;
import com.wolffsmod.npcs.teams.AllyNameTags;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * The Custom NPCs factions friendly to the local player, as the server last sent them, which decide whether the player sees
 * the name tag of an NPC that shows it to allies only. Players in creative or spectator mode see every name tag.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientAllyFactions
{
    private static final Set<Integer> FRIENDLY = new HashSet<>();

    public static void receive(AllyFactionsPacket packet)
    {
        FRIENDLY.clear();
        FRIENDLY.addAll(packet.factions());
    }

    /** @return whether the local player is not shown the NPC's name tag, which only allies of its faction see */
    public static boolean hidesNameTag(EntityNPCInterface npc)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !AllyNameTags.alliesOnly(npc) || player.isCreative() || player.isSpectator())
            return false;
        return !FRIENDLY.contains(((AllyNameTags.Holder) npc).wolffsmodnpcsFactionId());
    }
}
