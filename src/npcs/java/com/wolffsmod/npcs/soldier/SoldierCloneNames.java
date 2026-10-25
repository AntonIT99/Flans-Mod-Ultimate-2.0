package com.wolffsmod.npcs.soldier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityCustomNpc;

import net.minecraft.nbt.CompoundTag;

/** Saved model preferences and entity identity distinguish clone creation from ordinary world reloads. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoldierCloneNames
{
    public static final String REGENERATE = "WolffsModRegenerateCloneName";
    public static final String GENDER = "WolffsModCloneNameGender";
    private static final String OWNER = "WolffsModNameOwner";

    public static int selectedGender(CompoundTag extra)
    {
        return extra.getInt(GENDER) == 2 ? 2 : 1;
    }

    public static void save(EntityCustomNpc npc, CompoundTag tag)
    {
        tag.putUUID(OWNER, npc.getUUID());
    }

    /** Clone storage removes the vanilla UUID, while this saved identity still identifies its source NPC. */
    public static void loaded(EntityCustomNpc npc, CompoundTag tag)
    {
        if (npc.level().isClientSide || !tag.hasUUID(OWNER) || tag.getUUID(OWNER).equals(npc.getUUID()) || !npc.modelData.extra.getBoolean(REGENERATE))
            return;
        FlanSoldierSelection.read(npc.modelData.extra).ifPresent(selection -> FlanSoldierNames.apply(npc, selection, selectedGender(npc.modelData.extra)));
    }
}
