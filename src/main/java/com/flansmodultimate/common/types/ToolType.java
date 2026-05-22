package com.flansmodultimate.common.types;

import com.flansmodultimate.common.recipe.RecipeParser;
import lombok.Getter;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;
import static com.flansmodultimate.util.TypeReaderUtils.readValues;

public class ToolType extends InfoType
{
    /** Boolean switches that decide whether the tool should heal players and / or driveables */
    @Getter
    protected boolean healPlayers;
    @Getter
    protected boolean healDriveables;
    /** The amount to heal per use (one use per click) */
    @Getter
    protected int healAmount;
    /** The amount of uses the tool has. 0 means infinite */
    @Getter
    protected int toolLife;
    /** If true, the tool will destroy itself when finished. Disable this for rechargeable tools */
    @Getter
    protected boolean destroyOnEmpty = true;
    /** The items required to be added (shapelessly) to recharge the tool */
    protected List<ItemStack> rechargeRecipe = new ArrayList<>();
    /** If true, then this tool will deploy a parachute upon use (and consume itself) */
    @Getter
    protected boolean parachute;
    /** If true, then this will detonate the least recently placed remote explosive */
    @Getter
    protected boolean remote;
    /** If > 0, then the player can eat this and recover this much hunger */
    @Getter
    protected int foodness;
    @Getter
    protected boolean key;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        parachute = readValue("Parachute", parachute, file);
        remote = readValue("ExplosiveRemote", remote, file);
        key = readValue("Key", key, file);
        healPlayers = readValue("Heal", healPlayers, file);
        healPlayers = readValue("HealPlayers", healPlayers, file);
        healDriveables = readValue("Repair", healDriveables, file);
        healDriveables = readValue("RepairVehicles", healDriveables, file);
        healAmount = readValue("HealAmount", toolLife, file);
        healAmount = readValue("RepairAmount", toolLife, file);
        toolLife = readValue("ToolLife", toolLife, file);
        toolLife = readValue("ToolUes", toolLife, file);
        destroyOnEmpty = readValue("DestroyOnEmpty", destroyOnEmpty, file);
        foodness = readValue("Food", foodness, file);
        foodness = readValue("Foodness", foodness, file);
        rechargeRecipe.addAll(RecipeParser.resolveAmountThenItemPairs(readValues("RechargeRecipe", file), 1, contentPack, file, "RechargeRecipe"));
    }
}
