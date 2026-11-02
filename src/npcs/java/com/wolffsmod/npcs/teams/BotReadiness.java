package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.wolffsmod.npcs.combat.NpcAmmunition;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponChoice;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Readiness is measured only during the existing commander pass, without changing equipment or ammunition. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotReadiness
{
    record Strength(double readiness, double antiArmour)
    {}

    static Strength measure(List<Entity> members, int places, BotRole role, long now)
    {
        double ready = 0D;
        double antiArmour = 0D;
        for (Entity member : members)
        {
            double contribution = health(member);
            if (member instanceof EntityNPCInterface npc)
            {
                contribution *= ammunition(npc);
                if (NpcSuppression.isPinned(npc))
                    contribution *= 0.35D;
                if (NpcTacticState.of(npc).isReloading(now))
                    contribution *= 0.75D;
                if (role == BotRole.ARMOR || NpcWeaponChoice.antiArmor(npc, NpcEquipment.rangedWeapon(npc)))
                    antiArmour += contribution;
            }
            ready += contribution;
        }
        double surviving = (double) members.size() / Math.max(1, places);
        return new Strength(ready / Math.max(1, members.size()) * Math.min(1D, surviving), antiArmour);
    }

    private static double health(Entity entity)
    {
        return entity instanceof LivingEntity living ? Math.max(0.1D, Math.min(1D, living.getHealth() / Math.max(1F, living.getMaxHealth()))) : 1D;
    }

    private static double ammunition(EntityNPCInterface npc)
    {
        ItemStack weapon = NpcEquipment.rangedWeapon(npc);
        var properties = FlansEquipment.getWeaponProperties(weapon, ItemStack.EMPTY);
        if (properties.isEmpty() || properties.get().throwable() || FlansEquipment.hasLoadedRound(weapon))
            return 1D;
        return NpcAmmunition.nothingToLoad(npc, weapon) ? 0.15D : 0.75D;
    }
}
