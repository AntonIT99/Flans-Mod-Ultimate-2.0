package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.tactics.NpcCombatAdvance;
import com.wolffsmod.npcs.tactics.NpcInfantryRole;
import com.wolffsmod.npcs.tactics.NpcSafetyState.Action;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcWeaponRange;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Caches squad-local value snapshots for five ticks, without level scans or retained entity references. Server thread. */
final class BotSquadCombat
{
    private final SquadCombat decisions = new SquadCombat();
    private List<SquadCombat.Member> members = List.of();
    private final Map<UUID, Integer> targets = new HashMap<>();
    private long sampledAt = Long.MIN_VALUE / 2;

    SquadCombat decisions()
    {
        return decisions;
    }

    List<SquadCombat.Member> members(EntityNPCInterface observer, BotBattle battle, BotSquad squad, long now)
    {
        LivingEntity enemy = observer.getTarget();
        int target = enemy == null ? -1 : enemy.getId();
        Integer sampledTarget = targets.get(observer.getUUID());
        if (now - sampledAt >= 5L || now < sampledAt || sampledTarget == null || sampledTarget != target)
        {
            sampledAt = now;
            members = observer.level() instanceof ServerLevel level ? sample(level, battle, squad, now) : List.of();
            targets.clear();
            members.forEach(member -> targets.put(member.id(), member.target()));
        }
        return members;
    }

    /** The commander reuses the combat cache instead of building a second set of member observations. */
    List<SquadCombat.Member> commandMembers(ServerLevel level, BotBattle battle, BotSquad squad, long now)
    {
        if (now - sampledAt >= 5L || now < sampledAt)
        {
            sampledAt = now;
            members = sample(level, battle, squad, now);
            targets.clear();
            members.forEach(member -> targets.put(member.id(), member.target()));
        }
        return members;
    }

    private static List<SquadCombat.Member> sample(ServerLevel level, BotBattle battle, BotSquad squad, long now)
    {
        List<SquadCombat.Member> sampled = new ArrayList<>();
        for (int slotId : squad.getSlots())
        {
            BotSlot slot = battle.getSlots().get(slotId);
            Entity entity = slot == null || slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity instanceof EntityNPCInterface npc && npc.isAlive() && !npc.isKilled() && !npc.isPassenger())
                sampled.add(snapshot(npc, now));
        }
        return List.copyOf(sampled);
    }

    static SquadCombat.Member snapshot(EntityNPCInterface npc, long now)
    {
        LivingEntity enemy = npc.getTarget();
        NpcTacticState state = NpcTacticState.of(npc);
        boolean moving = state.getSafety().active(now) && state.getSafety().getAction() == Action.ADVANCE;
        boolean reloading = state.isReloading(now);
        boolean pinned = NpcSuppression.isPinned(npc);
        boolean visible = NpcEquipment.enabled(npc, Feature.COVERING_FIRE) && enemy != null && enemy.isAlive() && enemy.level() == npc.level() && npc.getSensing().hasLineOfSight(enemy);
        boolean ready = !moving && !reloading && !pinned && visible && NpcEquipment.enabled(npc, Feature.COVERING_FIRE) && NpcEquipmentRangedGoal.engages(npc)
            && npc.isInRange(enemy, NpcWeaponRange.firing(npc)) && loaded(npc);
        var intent = NpcCombatAdvance.intent(npc, visible ? enemy.position() : null);
        boolean wantsAdvance = NpcCombatAdvance.enabled(npc) && intent != null && !intent.reached(npc.position()) && (reloading || now >= state.getSafety().getAdvanceReadyAt());
        BotSettings.ClassRole role = NpcInfantryRole.of(npc);
        if (role != BotSettings.ClassRole.ASSAULT && NpcInfantryRole.support(npc))
            role = BotSettings.ClassRole.SUPPORT;
        return new SquadCombat.Member(npc.getUUID(), npc.position(), enemy == null ? -1 : enemy.getId(), ready, moving, reloading, pinned, role, wantsAdvance, visible ? enemy.position() : null);
    }

    private static boolean loaded(EntityNPCInterface npc)
    {
        ItemStack weapon = NpcEquipment.rangedWeapon(npc);
        var properties = FlansEquipment.getWeaponProperties(weapon, ItemStack.EMPTY);
        return properties.isEmpty() || properties.get().throwable() || FlansEquipment.hasLoadedRound(weapon);
    }
}
