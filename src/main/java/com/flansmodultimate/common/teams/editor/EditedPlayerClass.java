package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.api.SkinOverrideVariant;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.PlayerClass;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Detached effective class, leaving the content-pack original intact. */
public final class EditedPlayerClass extends PlayerClass
{
    @Nullable
    private final PlayerClass original;
    private final JsonObject patch;

    public EditedPlayerClass(String id, @Nullable PlayerClass original, JsonObject patch)
    {
        this.original = original;
        this.patch = patch.deepCopy();
        uniqueShortName = id;
        originalShortName = original == null ? id : original.getOriginalShortName();
        contentPack = original == null ? EditorCatalog.CUSTOM_PACK : original.getContentPack();
        fileName = original == null ? id : original.getFileName();
        type = EnumType.PLAYER_CLASS;
        name = original == null ? id : original.getName();
        if (patch.has("name"))
            name = patch.get("name").getAsString();
    }

    @Override
    public int getUnlockLevel()
    {
        return number("unlockLevel", original == null ? 0 : original.getUnlockLevel()).intValue();
    }

    @Override
    public int getRequiredPrestigeLevel()
    {
        return number("requiredPrestigeLevel", original == null ? 0 : original.getRequiredPrestigeLevel()).intValue();
    }

    @Override
    public int getClassLimit()
    {
        return number("classLimit", original == null ? -1 : original.getClassLimit()).intValue();
    }

    @Override
    public float getClassLimitPercent()
    {
        return number("classLimitPercent", original == null ? -1F : original.getClassLimitPercent()).floatValue();
    }

    private Number number(String field, Number fallback)
    {
        return patch.has(field) ? patch.get(field).getAsNumber() : fallback;
    }

    @Override
    public boolean usesTeamArmour(EquipmentSlot slot)
    {
        JsonObject armour = patch.getAsJsonObject("armour");
        if (armour == null || !armour.has(slot.getName()))
            return true;
        JsonElement piece = armour.get(slot.getName());
        return piece.isJsonPrimitive() && "inherit".equals(piece.getAsString());
    }

    @Override
    public ItemStack getArmour(EquipmentSlot slot)
    {
        return EditorStacks.armour(patch, slot, () -> original == null ? ItemStack.EMPTY : original.getArmour(slot));
    }

    @Override
    public Map<Integer, ItemStack> createStartingInventory()
    {
        JsonObject items = patch.getAsJsonObject("items");
        if (items == null)
            return Map.of();
        Map<Integer, ItemStack> result = new TreeMap<>();
        items.entrySet().forEach(entry -> result.put(Integer.parseInt(entry.getKey()), EditorStacks.load(entry.getValue().getAsJsonObject())));
        return Collections.unmodifiableMap(result);
    }

    @Override
    public List<ItemStack> createStartingItems()
    {
        if (!patch.has("items"))
            return original == null ? List.of() : original.createStartingItems();
        return createStartingInventory().values().stream().filter(stack -> !stack.isEmpty()).toList();
    }

    @Override
    public List<ItemStack> createStartingItemPreviews()
    {
        return createStartingItems();
    }

    @Override
    public List<SkinOverrideVariant> getSkinOverrideVariants()
    {
        if (!patch.has("skins"))
            return original == null ? List.of() : original.getSkinOverrideVariants();
        List<SkinOverrideVariant> skins = new ArrayList<>();
        for (JsonElement entry : patch.getAsJsonArray("skins"))
        {
            ResourceLocation texture = ResourceLocation.tryParse(entry.getAsString());
            skins.add(new SkinOverrideVariant(entry.getAsString(), texture));
        }
        return List.copyOf(skins);
    }

    @Override
    public String getSkinOverride()
    {
        List<SkinOverrideVariant> skins = getSkinOverrideVariants();
        return skins.isEmpty() ? "" : skins.get(0).name();
    }

    @Override
    @Nullable
    public ResourceLocation getSkinOverrideTexture()
    {
        List<SkinOverrideVariant> skins = getSkinOverrideVariants();
        return skins.isEmpty() ? null : skins.get(0).texture();
    }
}
