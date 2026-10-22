package com.wolffsmod.npcs.model;

import com.flansmodultimate.api.IContentType;
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
    static EntityCustomNpc replace(ServerLevel level, FlanModelEntity summoned)
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
        else
            // A dedicated server knows no content-pack textures; each client puts the model's own texture on.
            NpcSkins.markModelSkin(npc);
        if (summoned.hasCustomName() && summoned.getCustomName() != null)
            npc.display.setName(summoned.getCustomName().getString());
        else
            npc.display.setName(defaultName(summoned));
        level.addFreshEntity(npc);
        npc.setHealth(npc.getMaxHealth());
        return npc;
    }

    /**
     * The vehicle's localized name where the server can translate it, as with a player hosting the game; a dedicated
     * server has no translations, and takes the name the definition declares instead of the untranslated key.
     */
    private static String defaultName(FlanModelEntity summoned)
    {
        String localized = summoned.getType().getDescription().getString();
        IContentType definition = summoned.getInfoType();
        boolean untranslated = localized.equals(summoned.getType().getDescriptionId());
        return untranslated && definition != null && !definition.getName().isBlank() ? definition.getName() : localized;
    }
}
