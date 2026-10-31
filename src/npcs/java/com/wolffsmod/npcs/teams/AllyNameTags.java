package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.network.AllyFactionsPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;

/**
 * Name tags only allies see, for NPCs that are no Teams bots: an NPC whose switch is on tells clients so with its spawn
 * data, and every player learns, whenever it changes, which Custom NPCs factions are friendly to them; a client then hides
 * the name tag of such an NPC whose faction is not among them. Teams bots follow their battle's name tag rule instead.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AllyNameTags
{
    /** Key of the switch in an NPC's spawn data, and in the client's persistent data of the NPC. */
    public static final String KEY = "WolffsModAllyNameTags";
    /** Ticks between two looks at which factions are friendly to each player. */
    private static final int INTERVAL = 20;
    /** The friendly factions each player was last sent; a player who logs in again is a new entity and is sent them afresh. */
    private static final Map<ServerPlayer, Set<Integer>> SENT = new WeakHashMap<>();

    /** An NPC whose faction clients read from its synchronized data. */
    public interface Holder
    {
        /** @return the id of the NPC's Custom NPCs faction, as the server last synchronized it */
        int wolffsmodnpcsFactionId();
    }

    /** Adds the switch to an NPC's spawn data while it is on; nothing for a Teams bot. Server side. */
    public static void writeClient(EntityNPCInterface npc, CompoundTag spawnData)
    {
        if (!BotTag.isBot(npc) && NpcEquipment.enabled(npc, Feature.ALLY_NAME_TAGS))
            spawnData.putBoolean(KEY, true);
    }

    /** Keeps the switch the server sent with the client's copy of the NPC. Client side. */
    public static void readClient(Entity npc, CompoundTag spawnData)
    {
        if (spawnData.getBoolean(KEY))
            npc.getPersistentData().putBoolean(KEY, true);
        else
            npc.getPersistentData().remove(KEY);
    }

    /** @return whether only allies see the NPC's name tag, on the client */
    public static boolean alliesOnly(Entity npc)
    {
        return npc.getPersistentData().getBoolean(KEY);
    }

    /** Sends each player the factions friendly to them whenever those change; once a second. */
    public static void serverTick(MinecraftServer server)
    {
        if (server.getTickCount() % INTERVAL != 0)
            return;
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            Set<Integer> friendly = friendlyFactions(player);
            if (!friendly.equals(SENT.get(player)))
            {
                SENT.put(player, friendly);
                NpcNetwork.sendTo(player, new AllyFactionsPacket(List.copyOf(friendly)));
            }
        }
    }

    public static void serverStopping()
    {
        SENT.clear();
    }

    private static Set<Integer> friendlyFactions(ServerPlayer player)
    {
        Set<Integer> friendly = new HashSet<>();
        for (Faction faction : FactionController.instance.factions.values())
        {
            if (faction.isFriendlyToPlayer(player))
                friendly.add(faction.id);
        }
        return friendly;
    }
}
