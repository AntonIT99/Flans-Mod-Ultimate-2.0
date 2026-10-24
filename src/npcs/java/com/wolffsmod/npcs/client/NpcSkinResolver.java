package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.teams.SkinOverrideVariant;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.NpcSkins;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomEntities;
import noppes.npcs.entity.EntityCustomNpc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Objects;

/**
 * Puts the skins the server could only mark on the client's copies of NPCs: a class's skin, on the 64x32 model when it
 * is an old 2:1 sheet, and a vehicle model's texture in its paintjob. Applied each time the server sends the NPC, which
 * resets it to what the server holds. Client side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSkinResolver
{
    public static void apply(EntityCustomNpc npc)
    {
        if (NpcSkins.applies(npc, NpcSkins.MODEL_SKIN_KEY))
            applyModelTexture(npc);
        else if (NpcSkins.applies(npc, NpcSkins.CLASS_SKIN_KEY))
            applyClassSkin(npc);
    }

    private static void applyModelTexture(EntityCustomNpc npc)
    {
        if (npc.modelData.getEntity(npc) instanceof FlanModelEntity model && model.getModelTexture() != null)
            wear(npc, model.getModelTexture());
    }

    private static void applyClassSkin(EntityCustomNpc npc)
    {
        FlanSoldierSelection selection = FlanSoldierSelection.read(npc.modelData.extra).orElse(null);
        if (selection == null)
            return;
        List<SkinOverrideVariant> variants = selection.playerClass().getSkinOverrideVariants();
        ResourceLocation skin = selection.skin() < variants.size() ? variants.get(selection.skin()).texture() : null;
        FlanSoldierBrowser.SkinLayout layout = FlanSoldierBrowser.layout(skin);
        if (skin == null || layout == FlanSoldierBrowser.SkinLayout.NONE)
            return;
        // An old 2:1 sheet needs Custom NPCs' 64x32 model, as the model picker gives it. Picking a model empties the extra
        // data, which still holds the selection and the mark, so it is put back.
        ResourceLocation legacyModel = EntityType.getKey(CustomEntities.entityNPC64x32);
        ResourceLocation wanted = layout == FlanSoldierBrowser.SkinLayout.LEGACY ? legacyModel : null;
        if (!Objects.equals(npc.modelData.getEntityName(), wanted))
        {
            CompoundTag extra = npc.modelData.extra.copy();
            npc.modelData.setEntity(wanted);
            npc.modelData.extra = extra;
        }
        wear(npc, skin);
    }

    private static void wear(EntityCustomNpc npc, ResourceLocation texture)
    {
        npc.display.setSkinTexture(texture.toString());
        // Custom NPCs keeps the texture it resolved last; it resolves the new skin on the next frame.
        npc.textureLocation = null;
    }
}
