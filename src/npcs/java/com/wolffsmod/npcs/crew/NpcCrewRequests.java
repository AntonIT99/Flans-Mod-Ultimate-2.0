package com.wolffsmod.npcs.crew;

import com.wolffsmod.npcs.network.CrewResultPacket;
import com.wolffsmod.npcs.network.CrewResultPacket.Result;
import com.wolffsmod.npcs.network.NpcNetwork;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** A builder's requests from the Crew tab, checked against what Custom NPCs lets the builder do. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcCrewRequests
{
    /**
     * Crews the empty seats of the NPC the player is editing, or removes its crew. The NPC must be the one the player is
     * editing, which Custom NPCs only records for a player allowed to open its editor, and the player needs the wand and
     * the permissions Custom NPCs asks for to change an NPC's statistics and to create or delete NPCs.
     *
     * @param entityId
     *            the NPC the request was made for
     * @param settings
     *            the crew settings as edited, which the NPC takes before it is crewed
     */
    public static void request(ServerPlayer player, int entityId, boolean crew, CompoundTag settings)
    {
        EntityNPCInterface vehicle = NoppesUtilServer.getEditingNpc(player);
        boolean allowed = vehicle != null && vehicle.getId() == entityId && vehicle.isAlive() && vehicle.level() == player.level() && player.getInventory().getSelected().getItem() == CustomItems.wand
            && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_STATS)
            && CustomNpcsPermissions.hasPermission(player, crew ? CustomNpcsPermissions.NPC_CREATE : CustomNpcsPermissions.NPC_DELETE);
        if (!allowed)
        {
            reply(player, entityId, Result.DENIED, 0);
            return;
        }
        if (!crew)
        {
            reply(player, entityId, Result.REMOVED, NpcCrew.dismiss(vehicle));
            return;
        }
        NpcCrewSettings own = NpcCrewSettings.of(vehicle);
        if (NpcCrew.seats(vehicle).isEmpty())
        {
            reply(player, entityId, Result.NO_SEATS, 0);
            return;
        }
        own.read(settings);
        vehicle.updateClient = true;
        int boarded = NpcCrew.fill((ServerLevel) vehicle.level(), vehicle, NpcCrew.recruiter(own));
        // Crewed by hand, it does not crew itself again until it next spawns or comes back.
        ((NpcCrewHolder) vehicle).wolffsmodnpcsCrewedAs(vehicle.getUUID());
        reply(player, entityId, Result.CREWED, boarded);
    }

    private static void reply(ServerPlayer player, int entityId, Result result, int count)
    {
        CrewResultPacket packet = new CrewResultPacket(entityId, result, count);
        NpcNetwork.sendTo(player, packet);
        player.sendSystemMessage(packet.message());
    }
}
