package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.tactics.NpcCombatAdvance;
import com.wolffsmod.npcs.tactics.NpcCombatIntent;
import com.wolffsmod.npcs.tactics.NpcCombatMovement;
import com.wolffsmod.npcs.tactics.NpcInfantryRole;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.teams.BotSettings.ClassRole;
import com.wolffsmod.npcs.teams.NpcDuties;
import com.wolffsmod.npcs.teams.NpcSuppression;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/** Melee soldiers close while an opening javelin throw runs independently, then use ordinary item melee attacks. */
public final class NpcMeleeRoleGoal extends Goal
{
    private final EntityNPCInterface npc;
    private long attackAt;
    private long repathAt;

    public NpcMeleeRoleGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        LivingEntity target = npc.getTarget();
        return automatic(npc) && target != null && target.isAlive() && target.level() == npc.level() && hasMelee(npc);
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        LivingEntity target = npc.getTarget();
        if (target == null)
            return;
        long now = npc.level().getGameTime();
        if (committed(npc, target) || !NpcEquipment.meleeWeapon(NpcEquipment.meleeWeapon(npc)))
            selectMelee(npc);
        npc.getLookControl().setLookAt(target, 30F, 30F);
        double reach = NpcEquipment.itemMelee(npc, EquipmentProperty.MELEE_REACH) ? NpcEquipmentStats.meleeReach(NpcEquipment.meleeWeapon(npc)) : npc.stats.melee.getRange();
        boolean inReach = npc.isInRange(target, Math.max(0D, reach));
        if (inReach || NpcSuppression.isPinned(npc) || !permits(target))
            npc.getNavigation().stop();
        else if (now >= repathAt && npc.getSensing().hasLineOfSight(target))
        {
            repathAt = now + 10L;
            npc.getNavigation().moveTo(target, 1D);
        }
        if (inReach && now >= attackAt && npc.getSensing().hasLineOfSight(target))
        {
            int delay = NpcEquipment.itemMelee(npc, EquipmentProperty.MELEE_DELAY) ? NpcEquipment.meleeDelay(NpcEquipment.meleeWeapon(npc)) : npc.stats.melee.getDelay();
            attackAt = now + Math.max(1, delay);
            npc.swing(NpcEquipment.meleeHand(npc));
            npc.doHurtTarget(target);
        }
    }

    @Override
    public void stop()
    {
        npc.getNavigation().stop();
    }

    /** One opening throw per target; near melee the weapon is retained for striking. */
    public static boolean committed(EntityNPCInterface npc, LivingEntity target)
    {
        return automatic(npc) && hasMelee(npc) && (npc.isInRange(target, 8D) || target.getUUID().equals(NpcTacticState.of(npc).getMeleeThrowTarget()));
    }

    private static boolean automatic(EntityNPCInterface npc)
    {
        return NpcInfantryRole.of(npc) == ClassRole.MELEE && TacticKind.of(npc) == TacticKind.STANDARD && !npc.isPassenger() && NpcTuning.of(npc).tactic() == NpcTuning.Tactic.CUSTOM_NPCS
            && CustomNpcsPlatform.tacticalVariant(npc) == 0 && NpcCombatMovement.allowed(npc) && !NpcDuties.retreating(npc);
    }

    private boolean permits(LivingEntity target)
    {
        NpcCombatIntent assigned = NpcCombatAdvance.assigned(npc);
        return NpcCombatMovement.permits(npc, target.position()) && (assigned == null || assigned.permits(npc.position(), target.position()));
    }

    private static boolean hasMelee(EntityNPCInterface npc)
    {
        return NpcEquipment.meleeWeapon(NpcEquipment.meleeWeapon(npc)) || NpcAmmunition.inventory(npc).stream().anyMatch(NpcEquipment::meleeWeapon);
    }

    private static void selectMelee(EntityNPCInterface npc)
    {
        var items = NpcAmmunition.inventory(npc);
        double current = meleeScore(npc.getMainHandItem());
        int slot = NpcAmmunition.best(items, NpcMeleeRoleGoal::meleeScore, Math.max(0D, current));
        if (slot >= 0)
        {
            NpcAmmunition.swap(npc, 0, slot);
            npc.updateClient = true;
        }
    }

    private static double meleeScore(ItemStack item)
    {
        return NpcEquipment.meleeWeapon(item) ? NpcEquipment.meleeDamage(item) : -1D;
    }
}
