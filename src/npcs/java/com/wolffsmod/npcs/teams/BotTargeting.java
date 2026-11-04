package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.NpcAirCombat;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcVehicleCombat;
import com.wolffsmod.npcs.combat.NpcWeaponChoice;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import com.wolffsmod.npcs.tactics.NpcInfantryAirDefense;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Which enemies soldiers look at first: anti-tank soldiers go for armour, everyone else for infantry, so riflemen do not
 * waste themselves on tanks while there is infantry to fight. Soldier bots always do; any other NPC on foot while its
 * switch is on, an anti-tank soldier by its duty or its equipment. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotTargeting
{
    private static final int FIRST = 0;
    private static final int LATER = 1;

    /** @return 0 for a candidate a soldier looks at first, 1 for one it looks at after those; 0 for every candidate of anyone else */
    static int rank(EntityNPCInterface npc, LivingEntity candidate)
    {
        if (NpcAirCombat.active(npc))
            return NpcAirCombat.of(npc).rank(npc, candidate);
        if (TacticKind.of(npc) == TacticKind.AA_GUN && NpcInfantryAirDefense.airborne(candidate))
            return TeamsBots.reportedAirThreat(npc, candidate) ? -2 : -1;
        if (NpcInfantryAirDefense.holdsPosition(npc, candidate) && NpcEquipment.enabled(npc, Feature.SUITED_TARGETS))
            return NpcInfantryAirDefense.canEngage(npc, candidate) ? LATER : 3;
        if (NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT) && NpcVehicleThreats.groundVehicle(npc))
            return NpcVehicleCombat.threatRank(npc, candidate);
        if (NpcEquipment.enabled(npc, Feature.SUITED_TARGETS) && NpcVehicleThreats.groundVehicle(candidate)
            && NpcWeaponChoice.effectiveness(npc, candidate) == NpcWeaponChoice.Effectiveness.INEFFECTIVE)
            return 2;
        return groundRank(npc, candidate);
    }

    private static int groundRank(EntityNPCInterface npc, LivingEntity candidate)
    {
        Optional<BotBattle> current = BotBattles.of(npc);
        if (current.isEmpty())
        {
            if (!NpcEquipment.enabled(npc, Feature.SUITED_TARGETS) || BotTag.isBot(npc) || TacticKind.of(npc) != TacticKind.STANDARD)
                return FIRST;
            boolean antiTank = NpcDuties.role(npc) == BotSettings.ClassRole.ANTI_TANK;
            return NpcDuties.armoured(candidate) == antiTank ? FIRST : LATER;
        }
        BotBattle battle = current.get();
        BotSlot slot = BotBattles.slotOf(battle, npc).orElse(null);
        if (slot == null || slot.isVehicle())
            return FIRST;
        boolean antiTank = BotRoles.of(battle.getSettings().team(slot.getTeam()), slot.getRole()) == BotSettings.ClassRole.ANTI_TANK;
        return armoured(battle, candidate) == antiTank ? FIRST : LATER;
    }

    /** Whether an entity is armour: a vehicle or mecha bot, or else armour as anyone outside the battle counts. Aircraft are not. */
    static boolean armoured(BotBattle battle, Entity entity)
    {
        Integer slotId = battle.getSlotOfEntity().get(entity.getUUID());
        BotSlot slot = slotId == null ? null : battle.getSlots().get(slotId);
        if (slot != null && slot.isVehicle())
        {
            BotSquad squad = battle.getSquads().get(slot.getSquad());
            return squad == null || squad.getRole() == BotRole.ARMOR || squad.getRole() == BotRole.NAVAL || squad.getRole() == BotRole.AA;
        }
        return NpcDuties.armoured(entity);
    }

    /** Whether someone rides a vehicle or mecha. */
    static boolean ridesArmour(Entity entity)
    {
        Entity vehicle = entity.getRootVehicle();
        if (vehicle == entity)
            return false;
        DriveableKind kind = FlansEntityTypes.getDriveableType(vehicle).map(IDriveableType::getDriveableKind).orElse(null);
        return kind == DriveableKind.VEHICLE || kind == DriveableKind.MECHA;
    }
}
