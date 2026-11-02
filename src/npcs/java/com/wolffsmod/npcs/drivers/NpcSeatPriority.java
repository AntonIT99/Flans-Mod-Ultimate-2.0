package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.driveables.IEmplacement;
import com.flansmodultimate.api.driveables.ISeat;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.teams.TeamsBots;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Allied players take precedence over NPC operators without changing faction or mount access rules. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSeatPriority
{
    public static boolean mayYield(EntityNPCInterface npc, Player player, boolean deployment)
    {
        return npc.faction != null && !npc.level().isClientSide && npc.isAlive() && !npc.isKilled() && !player.isSpectator() && NpcDrivers.ally(npc, player)
            && (!deployment || NpcEquipment.enabled(npc, Feature.PLAYER_SEAT_DEPLOYMENT) && TeamsBots.playerSeatDeployment(npc));
    }

    public static boolean giveWay(EntityNPCInterface npc, Player player, boolean deployment)
    {
        if (!mayYield(npc, player, deployment))
            return false;
        Entity mount = npc.getVehicle();
        if (mount instanceof ISeat)
            return NpcDrivers.yieldSeat(npc);
        if (mount instanceof EntityNPCInterface vehicle)
            return NpcCrew.yieldSeat(vehicle, npc);
        if (!(mount instanceof IEmplacement emplacement))
            return false;
        emplacement.setEmplacementFiring(false);
        NpcDriver driver = NpcDriver.of(npc);
        long now = npc.level().getGameTime();
        driver.setBoardAfter(now + 100);
        driver.refuse(mount.getUUID(), now + 1200);
        npc.stopRiding();
        return true;
    }
}
