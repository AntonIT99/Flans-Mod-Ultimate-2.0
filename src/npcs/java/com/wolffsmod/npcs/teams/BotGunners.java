package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.teams.FlansTeams;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;

/** Explicit player orders for allied soldiers manning real driveables. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotGunners
{
    private static final double BOARD_RANGE = 16D;

    /**
     * Orders the player's allied bots nearby into the free gunner seats of the driveable the player drives.
     *
     * @return how many bots boarded, or -1 when the player drives no driveable
     */
    public static int board(ServerPlayer player)
    {
        if (!(player.getVehicle() instanceof ISeat driverSeat) || !driverSeat.isDriverSeat() || driverSeat.getDriveable() == null)
            return -1;
        Entity driveable = driverSeat.getDriveable();
        List<Entity> seats = new ArrayList<>(player.level().getEntities(driveable, driveable.getBoundingBox().inflate(8D),
            entity -> entity instanceof ISeat seat && seat.getDriveable() == driveable && !seat.isDriverSeat() && seat.getRiddenByEntity() == null && seat.getGunType().isPresent()));
        if (seats.isEmpty())
            return 0;
        List<EntityNPCInterface> bots = player.level().getEntitiesOfClass(EntityNPCInterface.class, player.getBoundingBox().inflate(BOARD_RANGE),
            npc -> npc.isAlive() && !npc.isPassenger() && BotTag.read(npc).map(data -> !data.vehicle()).orElse(false) && allied(player, npc));
        List<EntityNPCInterface> nearest = new ArrayList<>(bots);
        nearest.sort(Comparator.comparingDouble(npc -> npc.distanceToSqr(player)));
        int boarded = 0;
        for (Entity seat : seats)
        {
            Iterator<EntityNPCInterface> candidates = nearest.iterator();
            while (candidates.hasNext())
            {
                EntityNPCInterface npc = candidates.next();
                if (NpcDrivers.boardOnOrder(npc, seat))
                {
                    candidates.remove();
                    boarded++;
                    break;
                }
            }
        }
        return boarded;
    }

    /**
     * Orders the bots in the driveable the player rides out of it.
     *
     * @return how many bots got off, or -1 when the player rides no driveable
     */
    public static int dismount(ServerPlayer player)
    {
        if (!(player.getVehicle() instanceof ISeat ownSeat) || ownSeat.getDriveable() == null)
            return -1;
        Entity driveable = ownSeat.getDriveable();
        int dismounted = 0;
        for (Entity entity : player.level().getEntities(driveable, driveable.getBoundingBox().inflate(8D), candidate -> candidate instanceof ISeat seat && seat.getDriveable() == driveable))
        {
            Entity rider = ((ISeat) entity).getRiddenByEntity();
            if (rider instanceof EntityNPCInterface npc && BotTag.isBot(npc) && allied(player, npc))
            {
                ((ISeat) entity).setAutomatedTriggers(false, false);
                NpcDrivers.disembark(npc);
                dismounted++;
            }
        }
        return dismounted;
    }

    private static boolean allied(ServerPlayer player, EntityNPCInterface npc)
    {
        return FlansTeams.getTeam(player)
            .flatMap(playerTeam -> FlansTeams.getTeam(npc).flatMap(npcTeam -> FlansTeams.getRunningRound().map(round -> round.getRound().areTeamsAllied(playerTeam, npcTeam)))).orElse(false);
    }
}
