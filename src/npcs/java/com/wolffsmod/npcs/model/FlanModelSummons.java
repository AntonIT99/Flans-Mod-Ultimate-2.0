package com.wolffsmod.npcs.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomEntities;
import noppes.npcs.entity.EntityCustomNpc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * Turns a summoned model entity into the Custom NPC it stands for: {@code /summon wolffsmodnpcs:model/<short name>} and
 * spawners then place a vehicle NPC that can be edited like any other.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FlanModelSummons
{
    static void replace(ServerLevel level, FlanModelEntity summoned)
    {
        // The 1.21 entity registry is defaulted, so a registered model type always has a key.
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(summoned.getType());
        summoned.discard();
        EntityCustomNpc npc = new EntityCustomNpc(CustomEntities.entityCustomNpc, level);
        npc.moveTo(summoned.getX(), summoned.getY(), summoned.getZ(), summoned.getYRot(), 0F);
        npc.ais.setStartPos(summoned.blockPosition());
        npc.modelData.setEntity(id);
        npc.modelData.extra.putInt(FlanModelEntity.PAINTJOB_KEY, summoned.getPaintjobId());
        ResourceLocation texture = summoned.getModelTexture();
        if (texture != null)
            npc.display.setSkinTexture(texture.toString());
        if (summoned.hasCustomName() && summoned.getCustomName() != null)
            npc.display.setName(summoned.getCustomName().getString());
        else
            npc.display.setName(summoned.getType().getDescription().getString());
        level.addFreshEntity(npc);
        npc.setHealth(npc.getMaxHealth());
    }
}
