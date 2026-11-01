package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.model.NpcSkins;
import com.wolffsmod.npcs.soldier.FlanSoldierLoadout;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomEntities;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * The NPCs that screens show as previews of a Flan class or a saved clone: client entities that no world holds, never
 * spawned, equipped on the server or sent to it. Render thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class NpcPreviews
{
    /** @return an NPC wearing the selection's class skin and loadout; a random skin previews the first */
    static EntityCustomNpc ofClass(Level level, FlanSoldierSelection selection)
    {
        EntityCustomNpc model = new EntityCustomNpc(CustomEntities.entityCustomNpc, level);
        FlanSoldierLoadout.Equipment equipment = FlanSoldierLoadout.equipment(selection);
        model.inventory.armor = equipment.armour();
        model.inventory.weapons = equipment.weapons();
        selection.write(model.modelData.extra);
        NpcSkins.markClassSkin(model);
        NpcSkinResolver.apply(model);
        return model;
    }

    /** @return the NPC a clone's saved data makes, or empty for data that makes none */
    static Optional<EntityNPCInterface> ofClone(Level level, CompoundTag data)
    {
        if (data.isEmpty())
            return Optional.empty();
        Optional<EntityNPCInterface> npc = EntityType.create(data, level).filter(EntityNPCInterface.class::isInstance).map(EntityNPCInterface.class::cast);
        npc.ifPresent(NpcSkins::resolveOnClient);
        return npc;
    }
}
