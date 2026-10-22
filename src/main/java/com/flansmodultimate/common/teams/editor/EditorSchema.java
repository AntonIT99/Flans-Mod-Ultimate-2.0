package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.common.teams.TeamFlagUrl;
import com.google.gson.*;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** Strict schema validation before disk changes or runtime publication. */
public final class EditorSchema
{
    private static final Set<String> COMMON = Set.of("name", "disabled", "armour");
    private static final Set<String> TEAM = Set.of("colour", "textColour", "classes", "roundsGenerator", "victorySoundEnabled", "victorySound", "defeatSound", "victoryFlag");
    private static final Set<String> CLASS = Set.of("items", "skins", "unlockLevel", "requiredPrestigeLevel", "classLimit", "classLimitPercent");
    private static final Set<String> ARMOUR = Set.of("head", "chest", "legs", "feet");

    private EditorSchema()
    {}

    public static void validate(TeamEditorStore.Document document)
    {
        validateEntries(document.teams(), true);
        validateEntries(document.classes(), false);
    }

    private static void validateEntries(JsonObject entries, boolean team)
    {
        require(entries.size() <= 2048, "At most 2048 entries per file");
        entries.entrySet().forEach(entry ->
        {
            try
            {
                require(entry.getKey().matches("custom:[a-z0-9_./-]{1,100}|pack/[^/]{1,256}/[a-z0-9_./-]{1,128}"), "Invalid definition key");
                JsonObject json = entry.getValue().getAsJsonObject();
                for (String field : json.keySet())
                    require(COMMON.contains(field) || (team ? TEAM : CLASS).contains(field), "Unknown field: " + field);
                if (json.has("name"))
                    require(!string(json.get("name"), 128).isBlank(), "Name cannot be blank");
                for (String field : new String[]{"disabled", "roundsGenerator", "victorySoundEnabled"})
                    if (json.has(field))
                        require(json.get(field).isJsonPrimitive() && json.getAsJsonPrimitive(field).isBoolean(), field + " must be true or false");
                if (json.has("colour"))
                    require(string(json.get("colour"), 7).matches("#[0-9a-fA-F]{6}"), "Colour must be #RRGGBB");
                if (json.has("textColour"))
                {
                    ChatFormatting colour = ChatFormatting.getByName(string(json.get("textColour"), 32));
                    require(colour != null && colour.isColor(), "Unknown text colour");
                }
                for (String field : new String[]{"victorySound", "defeatSound"})
                    if (json.has(field))
                        string(json.get(field), 256);
                if (json.has("victoryFlag"))
                {
                    String flag = string(json.get("victoryFlag"), TeamFlagUrl.MAX_LENGTH);
                    if (TeamFlagUrl.isUrl(flag))
                        require(TeamFlagUrl.parse(flag) != null, "Invalid flag URL: use an http or https address");
                    else if (!flag.isEmpty())
                        require(flag.length() <= 256 && ResourceLocation.tryParse(flag) != null, "Invalid victory flag texture");
                }
                if (json.has("classes"))
                {
                    JsonArray classes = json.getAsJsonArray("classes");
                    require(classes.size() <= 512, "At most 512 classes per team");
                    classes.forEach(id -> require(!string(id, 512).isBlank(), "Empty class reference"));
                }
                if (json.has("skins"))
                {
                    JsonArray skins = json.getAsJsonArray("skins");
                    require(skins.size() <= 64, "At most 64 skins per class");
                    skins.forEach(id -> require(ResourceLocation.tryParse(string(id, 256)) != null, "Invalid skin texture"));
                }
                for (String field : new String[]{"unlockLevel", "requiredPrestigeLevel", "classLimit", "classLimitPercent"})
                {
                    if (!json.has(field))
                        continue;
                    double value = json.get(field).getAsDouble();
                    int minimum = field.startsWith("classLimit") ? -1 : 0;
                    int maximum = "classLimitPercent".equals(field) ? 100 : 1000000;
                    require(Double.isFinite(value) && value >= minimum && value <= maximum, field + " is out of range");
                    require("classLimitPercent".equals(field) || value == Math.rint(value), field + " must be a whole number");
                }
                if (json.has("armour"))
                {
                    JsonObject armour = json.getAsJsonObject("armour");
                    armour.entrySet().forEach(piece ->
                    {
                        require(ARMOUR.contains(piece.getKey()), "Unknown armour slot: " + piece.getKey());
                        if (piece.getValue().isJsonPrimitive())
                            require(Set.of("empty", "inherit").contains(piece.getValue().getAsString()), "Armour must be an item, empty or inherit");
                        else
                            stack(piece.getValue().getAsJsonObject());
                    });
                }
                if (json.has("items"))
                {
                    JsonObject items = json.getAsJsonObject("items");
                    require(items.size() <= 37, "At most 36 inventory slots and one offhand slot");
                    items.entrySet().forEach(item ->
                    {
                        int slot = Integer.parseInt(item.getKey());
                        require(Integer.toString(slot).equals(item.getKey()) && (slot >= 0 && slot < 36 || slot == 40), "Invalid inventory slot: " + item.getKey());
                        stack(item.getValue().getAsJsonObject());
                    });
                }
            }
            catch (RuntimeException exception)
            {
                throw new IllegalArgumentException((team ? "teams.json: " : "classes.json: ") + entry.getKey() + ": " + exception.getMessage(), exception);
            }
        });
    }

    private static void stack(JsonObject stack)
    {
        require(Set.of("item", "count", "nbt", "extra").containsAll(stack.keySet()), "Unknown item field");
        require(stack.has("item") && ResourceLocation.tryParse(string(stack.get("item"), 256)) != null, "Invalid item ID");
        if (stack.has("count"))
        {
            double count = stack.get("count").getAsDouble();
            require(count >= 1 && count <= 64 && count == Math.rint(count), "Item count must be 1 to 64");
        }
        for (String field : new String[]{"nbt", "extra"})
        {
            if (!stack.has(field))
                continue;
            try
            {
                TagParser.parseTag(string(stack.get(field), 65536));
            }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception)
            {
                throw new IllegalArgumentException("Invalid item NBT: " + exception.getMessage(), exception);
            }
        }
    }

    private static String string(JsonElement value, int limit)
    {
        require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(), "Expected a string");
        String text = value.getAsString();
        require(text.length() <= limit && text.chars().noneMatch(Character::isISOControl), "Text too long or contains control characters");
        return text;
    }

    private static void require(boolean condition, String message)
    {
        if (!condition)
            throw new IllegalArgumentException(message);
    }
}
