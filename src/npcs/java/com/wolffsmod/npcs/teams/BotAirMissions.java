package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.combat.NpcAirCombat;
import com.wolffsmod.npcs.sim.NpcTuning.AircraftRole;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Routes aircraft toward the side's last reported contacts, without querying hidden entities or loading chunks. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotAirMissions
{
    static NpcAirCombat.Mission assign(EntityNPCInterface npc, Vec3 patrol, boolean autonomous)
    {
        if (!autonomous)
            return new NpcAirCombat.Mission(patrol, 4);
        return BotBattles.of(npc).map(battle ->
        {
            int side = BotBattles.slotOf(battle, npc).map(slot -> battle.sideOf(slot.getTeam())).orElse(-1);
            List<BotIntelligence.Contact> contacts = battle.getIntelligence().contacts(side, npc.level().getGameTime()).stream()
                .filter(contact -> !contact.airborne() || NpcAirCombat.of(npc).canIntercept()).toList();
            return choose(contacts, NpcAirCombat.of(npc).role(npc), npc.position(), patrol);
        }).orElseGet(() -> new NpcAirCombat.Mission(patrol, 4));
    }

    static NpcAirCombat.Mission choose(List<BotIntelligence.Contact> contacts, AircraftRole role, Vec3 position, Vec3 patrol)
    {
        return contacts.stream().filter(contact -> contact.airborne() || contact.position().subtract(patrol).horizontalDistanceSqr() <= 192D * 192D)
            .min(Comparator.<BotIntelligence.Contact>comparingInt(contact -> NpcAirCombat.priority(role, contact.airborne(), contact.intercept()))
                .thenComparingDouble(contact -> position.distanceToSqr(contact.position()) + (contact.armoured() ? -8192D : 0D)))
            .map(contact -> new NpcAirCombat.Mission(contact.position(), NpcAirCombat.priority(role, contact.airborne(), contact.intercept()))).orElseGet(() -> new NpcAirCombat.Mission(patrol, 4));
    }
}
