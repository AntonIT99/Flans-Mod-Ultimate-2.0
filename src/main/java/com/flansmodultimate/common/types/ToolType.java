package com.flansmodultimate.common.types;

import lombok.Getter;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.*;

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
        rechargeRecipe.addAll(getRechargeRecipeStacks(readValues("RechargeRecipe", file), file));
    }

    protected List<ItemStack> getRechargeRecipeStacks(String[] split, TypeFile file)
    {
        int pairs = Math.max(0, (split.length - 1) / 2);
        List<ItemStack> stacks = new ArrayList<>(pairs);

        for (int i = 0; i < pairs; i++)
        {
            int amountIndex = 2 * i + 1;
            int itemIndex = 2 * i + 2;

            if (amountIndex >= split.length || itemIndex >= split.length)
                break;

            int amount;
            try
            {
                amount = Integer.parseInt(split[amountIndex].trim());
            }
            catch (Exception e)
            {
                logError("Invalid amount '" + split[amountIndex] + "' in RechargeRecipe", file);
                continue;
            }

            String token = split[itemIndex] == null ? "" : split[itemIndex].trim();
            if (token.isEmpty())
            {
                logError("Missing item token in RechargeRecipe", file);
                continue;
            }

            String itemName = token;
            int damage = 0;

            int dot = token.indexOf('.');
            if (dot >= 0)
            {
                itemName = token.substring(0, dot).trim();
                String dmgStr = (dot + 1 < token.length()) ? token.substring(dot + 1).trim() : "";
                if (!dmgStr.isEmpty())
                {
                    try
                    {
                        damage = Integer.parseInt(dmgStr);
                    }
                    catch (Exception e)
                    {
                        logError("Invalid damage '" + dmgStr + "' in RechargeRecipe", file);
                    }
                }
            }

            ItemStack recipeElement = getRecipeElement(itemName, amount, damage, contentPack);
            if (recipeElement != null)
                stacks.add(recipeElement);
            else
                logError("Could not find item for RechargeRecipe: '" + itemName + "'", file);
        }

        return stacks;
    }
}
