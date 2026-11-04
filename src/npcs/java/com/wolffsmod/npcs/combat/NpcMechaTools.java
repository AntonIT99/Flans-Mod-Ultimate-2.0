package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.IMechaEquipment;
import com.flansmodultimate.api.equipment.MechaMeleeProperties;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcModels;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Selects arm equipment deterministically, shared by model inheritance, combat and synchronized tools. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcMechaTools
{
    public static boolean isMecha(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        return model != null && FlanModelKind.of(model.getInfoType()) == FlanModelKind.MECHA;
    }

    /** Best reach, then damage per tick, then damage; inventory order breaks exact ties without moving any stack. */
    public static ItemStack findSword(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || FlanModelKind.of(model.getInfoType()) != FlanModelKind.MECHA)
            return ItemStack.EMPTY;
        ItemStack best = ItemStack.EMPTY;
        MechaMeleeProperties chosen = null;
        for (ItemStack stack : NpcAmmunition.supplies(npc))
        {
            var properties = FlansEquipment.getMechaMeleeProperties(model.getInfoType(), stack);
            if (properties.isPresent() && (chosen == null || compare(properties.get(), chosen) > 0))
            {
                best = stack;
                chosen = properties.get();
            }
        }
        return best;
    }

    static int compare(MechaMeleeProperties first, MechaMeleeProperties second)
    {
        int reach = Double.compare(first.reach(), second.reach());
        if (reach != 0)
            return reach;
        int rate = Double.compare(first.damage() / first.delay(), second.damage() / second.delay());
        return rate != 0 ? rate : Float.compare(first.damage(), second.damage());
    }

    /** Both model arms have independent display stacks: sword on the right, ranged weapon or spare tool on the left. */
    public static ItemStack hand(EntityNPCInterface npc, boolean left)
    {
        if (!isMecha(npc))
            return ItemStack.EMPTY;
        ItemStack sword = findSword(npc);
        ItemStack right = sword;
        if (right.isEmpty())
            right = NpcAmmunition.supplies(npc).stream().filter(FlansEquipment::isMechaHandTool).findFirst().orElse(ItemStack.EMPTY);
        if (!left)
            return right;
        ItemStack ranged = NpcEquipment.rangedWeapon(npc);
        if (FlansEquipment.getWeaponKind(ranged).filter(kind -> kind.ranged() && !kind.throwable() && !kind.shield()).isPresent())
            return ranged;
        for (ItemStack stack : NpcAmmunition.supplies(npc))
            if (FlansEquipment.isMechaHandTool(stack) && stack != right)
                return stack;
        return ItemStack.EMPTY;
    }

    /** The synchronized sword's effective statistics, on either side. */
    public static Optional<MechaMeleeProperties> properties(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null)
            return Optional.empty();
        ItemStack sword = npc instanceof IMechaEquipment equipment ? equipment.flansmodultimateMechaTool(false) : ItemStack.EMPTY;
        // Detached editor copies load the inventory but do not receive world entity-data updates.
        if (sword.isEmpty())
            sword = findSword(npc);
        return FlansEquipment.getMechaMeleeProperties(model.getInfoType(), sword);
    }
}
