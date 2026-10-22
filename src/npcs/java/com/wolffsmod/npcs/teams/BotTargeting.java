package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.DriveableKind;
import com.flansmodultimate.api.FlansEntityTypes;
import com.flansmodultimate.api.IDriveableType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Which enemies soldier bots look at first: anti-tank soldiers go for armour, everyone else for infantry, so riflemen do
 * not waste themselves on tanks while there is infantry to fight. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotTargeting
{
    private static final int FIRST = 0;
    private static final int LATER = 1;

    /** @return 0 for a candidate a bot looks at first, 1 for one it looks at after those; 0 for every NPC that is no soldier bot */
    static int rank(EntityNPCInterface npc, LivingEntity candidate)
    {
        Optional<BotBattle> current = BotBattles.of(npc);
        if (current.isEmpty())
            return FIRST;
        BotBattle battle = current.get();
        BotSlot slot = BotBattles.slotOf(battle, npc).orElse(null);
        if (slot == null || slot.isVehicle())
            return FIRST;
        boolean antiTank = BotRoles.of(battle.getSettings().team(slot.getTeam()), slot.getRole()) == BotSettings.ClassRole.ANTI_TANK;
        return armoured(battle, candidate) == antiTank ? FIRST : LATER;
    }

    /** Whether an entity is armour: a vehicle or mecha bot, or someone riding a vehicle or mecha. Aircraft are not. */
    static boolean armoured(BotBattle battle, Entity entity)
    {
        Integer slotId = battle.getSlotOfEntity().get(entity.getUUID());
        BotSlot slot = slotId == null ? null : battle.getSlots().get(slotId);
        if (slot != null && slot.isVehicle())
        {
            BotSquad squad = battle.getSquads().get(slot.getSquad());
            return squad == null || squad.getRole() == BotRole.ARMOR || squad.getRole() == BotRole.NAVAL || squad.getRole() == BotRole.AA;
        }
        Entity vehicle = entity.getRootVehicle();
        if (vehicle == entity)
            return false;
        DriveableKind kind = FlansEntityTypes.getDriveableType(vehicle).map(IDriveableType::getDriveableKind).orElse(null);
        return kind == DriveableKind.VEHICLE || kind == DriveableKind.MECHA;
    }
}
