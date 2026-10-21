package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.IContentType;
import com.wolffsmod.npcs.crew.NpcCrew;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Soldiers in the seats of vehicle bots, when the round's settings give vehicles crews. Each kind of seat is crewed with the
 * class the team's settings name for it, or one picked by the class weights; crew members are bots of the vehicle's place
 * in the force, which spend no tickets, count nothing on capture points and score for the vehicle. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotCrews
{
    /** Puts a crew into the seats of a vehicle bot that has just spawned. */
    static void crew(ServerLevel level, BotRound round, BotFactory.Order order, EntityNPCInterface vehicle, RandomGenerator random)
    {
        if (!round.getSettings().isVehicleCrews())
            return;
        NpcCrew.fill(level, vehicle, (crewLevel, crewed, seat) ->
        {
            String playerClass = playerClass(order, order.force().crewClass(seat.post()), random);
            return playerClass.isEmpty() ? Optional.empty() : BotFactory.crewman(crewLevel, order, playerClass, crewed.position(), random);
        });
    }

    /**
     * @return the class named for a kind of seat when the team offers it, none for {@link BotSettings.TeamForce#NO_CREW}, and
     *         otherwise one picked by the class weights; empty for none
     */
    private static String playerClass(BotFactory.Order order, String wanted, RandomGenerator random)
    {
        if (BotSettings.TeamForce.NO_CREW.equalsIgnoreCase(wanted))
            return "";
        if (order.team().getClasses().stream().map(IContentType::getShortName).anyMatch(wanted::equals))
            return wanted;
        return BotPopulation.pickClass(order.team(), order.force(), random);
    }

    /** @return the place in the force a crew member's vehicle fills, for scoring the crew's kills to the vehicle */
    static Optional<BotSlot> slotOf(BotRound round, EntityNPCInterface crew)
    {
        return BotTag.read(crew).filter(data -> data.crew() && data.instance().equals(round.getInstance())).map(data -> round.getSlots().get(data.slot()));
    }
}
