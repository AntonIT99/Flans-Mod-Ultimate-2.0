package com.wolffsmod.npcs.model;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.*;

class FlanModelRenderingTest
{
    private static final ResourceLocation FLAN_MODEL = ResourceLocation.fromNamespaceAndPath("wolffsmodnpcs", "model/tank");

    @Test
    void oldSavesAndSpawnUpdatesKeepTheirFormerUnscaledAppearanceWithoutMutatingInput()
    {
        for (String key : new String[]{"NpcModelData", "ModelData"})
        {
            CompoundTag original = npc(key, FLAN_MODEL.toString(), true, 15F);
            original.putString("Name", "Tank");
            original.getCompound(key).getCompound("ExtraData").putInt("FlanPaintjob", 2);
            CompoundTag migrated = FlanModelRendering.migrateNpc(original, key, FLAN_MODEL::equals);
            assertNotSame(original, migrated);
            assertFalse(migrated.getCompound(key).getBoolean("SimpleRender"));
            assertEquals(5F, migrated.getFloat("Size"));
            assertEquals("Tank", migrated.getString("Name"));
            assertEquals(2, migrated.getCompound(key).getCompound("ExtraData").getInt("FlanPaintjob"));
            assertTrue(original.getCompound(key).getBoolean("SimpleRender"));
            assertEquals(15F, original.getFloat("Size"));
            assertSame(migrated, FlanModelRendering.migrateNpc(migrated, key, FLAN_MODEL::equals));
        }
    }

    @Test
    void normalRenderingRetainsConfiguredSizeAndOtherEntityModelsKeepTheirRendererChoice()
    {
        CompoundTag normal = npc("NpcModelData", FLAN_MODEL.toString(), false, 12F);
        assertSame(normal, FlanModelRendering.migrateNpc(normal, "NpcModelData", FLAN_MODEL::equals));
        for (String id : new String[]{"minecraft:zombie", "customnpcs:customnpc64x32", "", "invalid model"})
        {
            CompoundTag other = npc("NpcModelData", id, true, 8F);
            assertSame(other, FlanModelRendering.migrateNpc(other, "NpcModelData", FLAN_MODEL::equals));
        }
        CompoundTag empty = new CompoundTag();
        assertSame(empty, FlanModelRendering.migrateNpc(empty, "NpcModelData", FLAN_MODEL::equals));
    }

    private static CompoundTag npc(String modelKey, String id, boolean simple, float size)
    {
        CompoundTag model = new CompoundTag();
        model.putString("EntityName", id);
        model.putBoolean("SimpleRender", simple);
        model.put("ExtraData", new CompoundTag());
        CompoundTag tag = new CompoundTag();
        tag.put(modelKey, model);
        tag.putFloat("Size", size);
        return tag;
    }
}
