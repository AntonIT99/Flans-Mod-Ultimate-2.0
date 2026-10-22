package com.wolffsmod.npcs.crew;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** A builder's requests from the Crew tab, checked against what Custom NPCs lets the builder do. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcCrewRequests
{
    private static final String PREFIX = "wolffsmodnpcs.crew.";

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
            player.sendSystemMessage(Component.translatable(PREFIX + "denied"));
            return;
        }
        if (!crew)
        {
            player.sendSystemMessage(Component.translatable(PREFIX + "removed", NpcCrew.dismiss(vehicle)));
            return;
        }
        NpcCrewSettings own = NpcCrewSettings.of(vehicle);
        own.read(settings);
        if (NpcCrew.seats(vehicle).isEmpty())
        {
            player.sendSystemMessage(Component.translatable(PREFIX + "no_seats"));
            return;
        }
        int boarded = NpcCrew.fill((ServerLevel) vehicle.level(), vehicle, NpcCrew.recruiter(own));
        // Crewed by hand, it does not crew itself again until it next spawns or comes back.
        ((NpcCrewHolder) vehicle).wolffsmodnpcsCrewedAs(vehicle.getUUID());
        player.sendSystemMessage(Component.translatable(PREFIX + "crewed", boarded));
    }
}
