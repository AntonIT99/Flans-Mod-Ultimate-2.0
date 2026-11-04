package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.tactics.NpcInfantryRole;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Support fire uses visible targets and ordinary ammunition, with a conservative corridor for advancing allies. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class NpcSupportFire
{
    static boolean active(EntityNPCInterface npc)
    {
        return !npc.isPassenger() && TacticKind.of(npc) == TacticKind.STANDARD && NpcEquipment.enabled(npc, NpcWeaponOptions.Feature.COVERING_FIRE) && NpcInfantryRole.support(npc);
    }

    static boolean clear(EntityNPCInterface npc, LivingEntity target)
    {
        if (!(npc.level() instanceof ServerLevel level))
            return false;
        Vec3 from = npc.getEyePosition();
        Vec3 to = target.getEyePosition();
        NpcCombatants entities = NpcCombatants.of(level);
        for (int index = 0; index < entities.size(); index++)
        {
            var ally = entities.get(index);
            if (ally.entity() != npc && ally.entity() != target && ally.entity().isAlive() && NpcDrivers.ally(npc, ally.entity()) && ally.bounds().inflate(0.5D).clip(from, to).isPresent())
                return false;
        }
        return true;
    }

    static boolean blocked(EntityNPCInterface npc, LivingEntity target)
    {
        return active(npc) && !clear(npc, target);
    }
}
