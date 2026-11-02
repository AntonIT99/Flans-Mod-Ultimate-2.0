package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.driveables.IFlanSeatOccupant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Cooperative occupant handoff; callers retain ownership of mount permissions and entry events. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SeatOccupants
{
    public static boolean canEnter(Entity mount, Player player, boolean deployment)
    {
        Entity occupant = mount.getPassengers().stream().findFirst().orElse(null);
        if (mount.level().isClientSide)
            return !player.isSpectator() && (occupant == null || occupant == player || occupant instanceof IFlanSeatOccupant);
        return !player.isSpectator() && (occupant == null || occupant == player || occupant instanceof IFlanSeatOccupant cooperative && cooperative.flansmodultimateMayYieldSeat(player, deployment));
    }

    public static boolean makeRoom(Entity mount, Player player, boolean deployment)
    {
        if (mount.level().isClientSide || !canEnter(mount, player, deployment))
            return false;
        Entity occupant = mount.getPassengers().stream().findFirst().orElse(null);
        if (occupant == null || occupant == player)
            return true;
        return occupant instanceof IFlanSeatOccupant cooperative && cooperative.flansmodultimateYieldSeat(player, deployment) && mount.getPassengers().isEmpty();
    }
}
