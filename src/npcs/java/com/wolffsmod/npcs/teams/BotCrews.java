package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.content.IContentType;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.FlanModelNpcs;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.List;
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
    static void crew(ServerLevel level, BotBattle battle, BotFactory.Order order, EntityNPCInterface vehicle, RandomGenerator random)
    {
        if (!battle.getSettings().isVehicleCrews())
            return;
        String definition = order.slot().getRole();
        var kind = FlanModelNpcs.type(definition).map(FlanModelEntityType::getKind).orElse(null);
        NpcCrew.fill(level, vehicle, (crewLevel, crewed, seat) ->
        {
            String playerClass = playerClass(battle, order, order.force().effectiveCrewClass(definition, kind, seat.post()), random);
            return playerClass.isEmpty() ? Optional.empty() : BotFactory.crewman(crewLevel, order, playerClass, crewed.position(), random);
        });
    }

    /**
     * @return the class named for a kind of seat when the team offers it, none for {@link BotSettings.TeamForce#NO_CREW}, and
     *         otherwise one picked by the class weights; empty for none
     */
    private static String playerClass(BotBattle battle, BotFactory.Order order, String wanted, RandomGenerator random)
    {
        return selectedClass(order.team().getClasses().stream().map(IContentType::getShortName).toList(), wanted, random)
            .orElseGet(() -> BotPopulation.pickClass(battle, order.team(), order.force(), random));
    }

    /** Empty optional requests the usual weighted fallback; an empty class leaves the seat uncrewed. */
    static Optional<String> selectedClass(List<String> offered, String wanted, RandomGenerator random)
    {
        if (BotSettings.TeamForce.NO_CREW.equalsIgnoreCase(wanted))
            return Optional.of("");
        List<String> choices = BotSettingsText.crewClasses(wanted).stream().filter(offered::contains).toList();
        return choices.isEmpty() ? Optional.empty() : Optional.of(choices.get(random.nextInt(choices.size())));
    }

    /** @return the place in the force a crew member's vehicle fills, for scoring the crew's kills to the vehicle */
    static Optional<BotSlot> slotOf(BotBattle battle, EntityNPCInterface crew)
    {
        return BotTag.read(crew).filter(data -> data.crew() && data.instance().equals(battle.getInstance())).map(data -> battle.getSlots().get(data.slot()));
    }
}
