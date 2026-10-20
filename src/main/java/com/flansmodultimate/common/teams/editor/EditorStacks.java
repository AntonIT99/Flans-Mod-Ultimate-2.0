package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.platform.item.ItemStackData;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/** Human-readable item IDs/counts with lossless SNBT for custom item data. */
@SuppressWarnings("deprecation") // Use the same static registry IDs as the version-specific ItemStackData serialization boundary.
public final class EditorStacks
{
    private EditorStacks()
    {}

    public static ItemStack armour(JsonObject patch, EquipmentSlot slot, Supplier<ItemStack> fallback)
    {
        JsonObject armour = patch.getAsJsonObject("armour");
        if (armour == null || !armour.has(slot.getName()))
            return fallback.get();
        var piece = armour.get(slot.getName());
        return piece.isJsonObject() ? load(piece.getAsJsonObject()) : ItemStack.EMPTY;
    }

    public static JsonObject save(ItemStack stack)
    {
        JsonObject json = new JsonObject();
        json.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        json.addProperty("count", stack.getCount());
        if (ItemStackData.has(stack))
            json.addProperty("nbt", ItemStackData.copy(stack).toString());
        var extra = ItemStackData.saveBuiltIn(stack);
        extra.remove("id");
        extra.remove("Count");
        extra.remove("tag");
        if (!extra.isEmpty())
            json.addProperty("extra", extra.toString());
        return json;
    }

    public static ItemStack load(JsonObject json)
    {
        ResourceLocation id = ResourceLocation.tryParse(json.get("item").getAsString());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || BuiltInRegistries.ITEM.get(id) == Items.AIR)
            return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id), json.has("count") ? json.get("count").getAsInt() : 1);
        if (json.has("nbt"))
        {
            try
            {
                ItemStackData.set(stack, TagParser.parseTag(json.get("nbt").getAsString()));
            }
            catch (CommandSyntaxException exception)
            {
                throw new IllegalArgumentException("Invalid item nbt: " + exception.getMessage(), exception);
            }
        }
        if (json.has("extra"))
        {
            try
            {
                var encoded = TagParser.parseTag(json.get("extra").getAsString());
                encoded.merge(ItemStackData.saveBuiltIn(stack));
                return ItemStackData.parseBuiltIn(encoded);
            }
            catch (CommandSyntaxException exception)
            {
                throw new IllegalArgumentException("Invalid extra item data: " + exception.getMessage(), exception);
            }
        }
        return stack;
    }
}
