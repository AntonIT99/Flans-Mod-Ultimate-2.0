package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.common.types.*;
import com.google.gson.*;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Shared GUI/command descriptions, using complete effective values for previews and duplication. */
public final class EditorDescriptions
{
    public static final List<EquipmentSlot> ARMOUR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private EditorDescriptions()
    {}

    public static JsonObject team(Team team)
    {
        JsonObject json = new JsonObject();
        json.addProperty("name", team.getName());
        json.addProperty("colour", String.format("#%06X", team.getTeamColour() & 0xFFFFFF));
        json.addProperty("textColour", team.getTextColour().getName());
        json.addProperty("roundsGenerator", team.isAllowedForRoundsGenerator());
        json.addProperty("victorySoundEnabled", team.isVictorySoundEnabled());
        json.addProperty("victorySound", team.getVictorySound());
        json.addProperty("defeatSound", team.getDefeatSound());
        json.addProperty("victoryFlag", Objects.requireNonNullElseGet(team.getVictoryFlagUrl(), () -> Objects.toString(team.getVictoryFlagTextureLocation(), "")));
        JsonArray classes = new JsonArray();
        team.getClasses().forEach(type -> classes.add(EditorCatalog.key(type)));
        json.add("classes", classes);
        JsonObject armour = new JsonObject();
        ARMOUR.forEach(slot -> armour.add(slot.getName(), piece(team.getArmour(slot), "empty")));
        json.add("armour", armour);
        return json;
    }

    public static JsonObject playerClass(PlayerClass type)
    {
        JsonObject json = new JsonObject();
        json.addProperty("name", type.getName());
        json.addProperty("unlockLevel", type.getUnlockLevel());
        json.addProperty("requiredPrestigeLevel", type.getRequiredPrestigeLevel());
        json.addProperty("classLimit", type.getClassLimit());
        json.addProperty("classLimitPercent", type.getClassLimitPercent());
        JsonArray skins = new JsonArray();
        type.getSkinOverrideVariants().forEach(skin ->
        {
            if (skin.texture() != null)
                skins.add(skin.texture().toString());
            else if (type.getContentPack() != EditorCatalog.CUSTOM_PACK)
                skins.add(InfoType.loadSkinTexture(skin.name(), type).toString());
        });
        json.add("skins", skins);
        JsonObject armour = new JsonObject();
        ARMOUR.forEach(slot -> armour.add(slot.getName(), piece(type.getArmour(slot), type.usesTeamArmour(slot) ? "inherit" : "empty")));
        json.add("armour", armour);
        JsonObject items = new JsonObject();
        Map<Integer, ItemStack> layout = type.createStartingInventory();
        if (!layout.isEmpty())
            layout.forEach((slot, stack) -> items.add(Integer.toString(slot), EditorStacks.save(stack)));
        else
        {
            int slot = 0;
            for (ItemStack stack : type.createStartingItems())
            {
                if (slot >= 36)
                    break;
                items.add(Integer.toString(slot++), EditorStacks.save(stack));
            }
        }
        json.add("items", items);
        return json;
    }

    private static JsonElement piece(ItemStack stack, String empty)
    {
        return stack.isEmpty() ? new JsonPrimitive(empty) : EditorStacks.save(stack);
    }

    /** Only changed fields become overrides; untouched pack fields keep following the pack. */
    public static JsonObject changes(JsonObject before, JsonObject after)
    {
        JsonObject changes = new JsonObject();
        after.entrySet().forEach(entry ->
        {
            if (entry.getValue().equals(before.get(entry.getKey())))
                return;
            if ("armour".equals(entry.getKey()))
            {
                JsonObject previous = before.has("armour") ? before.getAsJsonObject("armour") : new JsonObject();
                changes.add("armour", changes(previous, entry.getValue().getAsJsonObject()));
            }
            else
                changes.add(entry.getKey(), entry.getValue().deepCopy());
        });
        return changes;
    }

    public static void merge(JsonObject target, JsonObject changes)
    {
        changes.entrySet().forEach(entry ->
        {
            if ("armour".equals(entry.getKey()) && target.has("armour"))
                merge(target.getAsJsonObject("armour"), entry.getValue().getAsJsonObject());
            else
                target.add(entry.getKey(), entry.getValue().deepCopy());
        });
    }
}
