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

/** Selects one sword addon deterministically, shared by model inheritance, combat and the synchronized right-arm tool. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcMechaTools
{
    public static boolean isMecha(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        return model != null && FlanModelKind.of(model.getInfoType()) == FlanModelKind.MECHA;
    }

    /** The first sword: main hand, off hand, projectile slot, then the nine inventory slots; also available to inventory editor copies. */
    public static ItemStack findSword(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || FlanModelKind.of(model.getInfoType()) != FlanModelKind.MECHA)
            return ItemStack.EMPTY;
        for (ItemStack stack : java.util.List.of(npc.getMainHandItem(), npc.getOffhandItem(), NpcAmmunition.projectile(npc)))
            if (FlansEquipment.getMechaMeleeProperties(model.getInfoType(), stack).isPresent())
                return stack;
        return NpcAmmunition.inventory(npc).stream().filter(stack -> FlansEquipment.getMechaMeleeProperties(model.getInfoType(), stack).isPresent()).findFirst().orElse(ItemStack.EMPTY);
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
