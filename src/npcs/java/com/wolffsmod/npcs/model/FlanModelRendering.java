package com.wolffsmod.npcs.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.ModelData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Predicate;

/** Owns the supported renderer policy and migrates the former unscaled simple-render mode. Common-side safe. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlanModelRendering
{
    private static final String SIMPLE_RENDER = "SimpleRender";

    public static boolean manages(ModelData data)
    {
        return manages(data.getEntityName());
    }

    private static boolean manages(ResourceLocation id)
    {
        return id != null && BuiltInRegistries.ENTITY_TYPE.get(id) instanceof FlanModelEntityType;
    }

    /** Direct flag writes cannot enable the unsupported renderer for a Flan model. */
    public static void enforce(ModelData data)
    {
        if (manages(data))
            data.simpleRender = false;
    }

    /** Model-only presets and editor packets carry no display data; migrate their owner's size directly. */
    public static void loaded(ModelData data, CompoundTag tag)
    {
        if (manages(data))
        {
            if (tag.getBoolean(SIMPLE_RENDER))
                data.npc.display.setSize(5F);
            data.simpleRender = false;
        }
    }

    /** Full saves and spawn packets load display data after model data, so migrate both together before either is read. */
    public static CompoundTag migrateNpc(CompoundTag tag, String modelKey)
    {
        return migrateNpc(tag, modelKey, FlanModelRendering::manages);
    }

    static CompoundTag migrateNpc(CompoundTag tag, String modelKey, Predicate<ResourceLocation> managed)
    {
        CompoundTag model = tag.getCompound(modelKey);
        ResourceLocation id = ResourceLocation.tryParse(model.getString("EntityName"));
        if (!model.getBoolean(SIMPLE_RENDER) || id == null || !managed.test(id))
            return tag;
        CompoundTag migrated = tag.copy();
        migrated.getCompound(modelKey).putBoolean(SIMPLE_RENDER, false);
        migrated.putFloat("Size", 5F);
        return migrated;
    }
}
