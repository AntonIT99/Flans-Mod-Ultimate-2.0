package com.flansmodultimate.common.recipe;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.ModUtils;
import com.flansmodultimate.util.ResourceUtils;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RecipeResolver
{
    public static ItemStack resolve(String token, @Nullable IContentProvider provider)
    {
        if (StringUtils.isBlank(token))
            return ItemStack.EMPTY;

        return RecipeIngredient.parse(token.trim(), 1, provider).resolve();
    }

    public static ItemStack resolve(String id, int amount, int damage, @Nullable IContentProvider provider)
    {
        if (StringUtils.isBlank(id) || amount <= 0)
            return ItemStack.EMPTY;

        String rawId = id.trim();
        String sanitizedId = ResourceUtils.sanitize(rawId);
        ItemStack legacyStack = getLegacyRecipeElement(sanitizedId, amount, damage);
        if (!legacyStack.isEmpty())
            return legacyStack;

        Optional<ItemStack> stack = ModUtils.getItemStack(rawId, amount, damage);
        if (stack.isPresent())
            return stack.get();

        if (!rawId.equals(sanitizedId))
        {
            stack = ModUtils.getItemStack(sanitizedId, amount, damage);
            if (stack.isPresent())
                return stack.get();
        }

        String aliasedId = ContentManager.getShortnameAliasInContentPack(sanitizedId, provider);
        InfoType type = InfoType.getInfoType(aliasedId);
        stack = ModUtils.getItemStack(type, amount, damage);
        if (stack.isPresent())
            return stack.get();

        stack = ModUtils.getItemStack(FlansMod.FLANSMOD_ID + ":" + aliasedId, amount, damage);
        if (stack.isPresent())
            return stack.get();

        FlansMod.log.warn("Could not find {} in recipe", rawId);
        return ItemStack.EMPTY;
    }

    private static ItemStack getLegacyRecipeElement(String id, int amount, int damage)
    {
        return switch (id)
        {
            case "dooriron" -> stack(Items.IRON_DOOR, amount);
            case "doorwood" -> stack(Items.OAK_DOOR, amount);
            case "clayitem" -> stack(Items.CLAY_BALL, amount);
            case "iron_trapdoor" -> stack(Blocks.IRON_TRAPDOOR, amount);
            case "trapdoor" -> stack(Blocks.OAK_TRAPDOOR, amount);
            case "gunpowder", "sulphur" -> stack(Items.GUNPOWDER, amount);
            case "ingotiron", "iron", "ingotsteel", "ingotnickel", "ingotlead", "ingottin" -> stack(Items.IRON_INGOT, amount);
            case "ingotgold", "gold", "ingotelectrum", "ingotconstantan", "ingotsilver", "ingotbronze" -> stack(Items.GOLD_INGOT, amount);
            case "ingotcopper" -> stack(Items.COPPER_INGOT, amount);
            case "nuggetiron", "nuggetsteel", "nuggetnickel", "nuggetlead", "nuggettin", "nuggetcopper" -> stack(Items.IRON_NUGGET, amount);
            case "nuggetgold", "nuggetelectrum", "nuggetconstantan", "nuggetsilver", "nuggetbronze" -> stack(Items.GOLD_NUGGET, amount);
            case "blockiron", "blocksteel", "blocknickel", "blocklead", "blocktin" -> stack(Blocks.IRON_BLOCK, amount);
            case "blockgold", "blockelectrum", "blockconstantan", "blocksilver", "blockbronze" -> stack(Blocks.GOLD_BLOCK, amount);
            case "blockcopper" -> stack(Blocks.COPPER_BLOCK, amount);
            case "blockdiamond" -> stack(Blocks.DIAMOND_BLOCK, amount);
            case "blockemerald" -> stack(Blocks.EMERALD_BLOCK, amount);
            case "blockredstone" -> stack(Blocks.REDSTONE_BLOCK, amount);
            case "boat" -> stack(Items.OAK_BOAT, amount);
            case "log" -> stack(legacyLog(damage), amount);
            case "log2" -> stack(legacyLog2(damage), amount);
            case "wood", "planks", "treatedplanks" -> stack(legacyPlanks(damage), amount);
            case "cloth", "wool" -> stack(legacyWool(damage), amount);
            case "dyepowder" -> stack(legacyDye(damage), amount);
            case "yellowdust", "lightstone" -> stack(Items.GLOWSTONE_DUST, amount);
            case "slimeball" -> stack(Items.SLIME_BALL, amount);
            case "enderpearl" -> stack(Items.ENDER_PEARL, amount);
            case "reeds" -> stack(Items.SUGAR_CANE, amount);
            case "seeds" -> stack(Items.WHEAT_SEEDS, amount);
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack stack(ItemLike item, int amount)
    {
        return new ItemStack(item, amount);
    }

    private static ItemLike legacyLog(int damage)
    {
        return switch (damage)
        {
            case 1 -> Blocks.SPRUCE_LOG;
            case 2 -> Blocks.BIRCH_LOG;
            case 3 -> Blocks.JUNGLE_LOG;
            default -> Blocks.OAK_LOG;
        };
    }

    private static ItemLike legacyLog2(int damage)
    {
        return damage == 1 ? Blocks.DARK_OAK_LOG : Blocks.ACACIA_LOG;
    }

    private static ItemLike legacyPlanks(int damage)
    {
        return switch (damage)
        {
            case 1 -> Blocks.SPRUCE_PLANKS;
            case 2 -> Blocks.BIRCH_PLANKS;
            case 3 -> Blocks.JUNGLE_PLANKS;
            case 4 -> Blocks.ACACIA_PLANKS;
            case 5 -> Blocks.DARK_OAK_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
    }

    private static ItemLike legacyWool(int damage)
    {
        return switch (damage)
        {
            case 1 -> Blocks.ORANGE_WOOL;
            case 2 -> Blocks.MAGENTA_WOOL;
            case 3 -> Blocks.LIGHT_BLUE_WOOL;
            case 4 -> Blocks.YELLOW_WOOL;
            case 5 -> Blocks.LIME_WOOL;
            case 6 -> Blocks.PINK_WOOL;
            case 7 -> Blocks.GRAY_WOOL;
            case 8 -> Blocks.LIGHT_GRAY_WOOL;
            case 9 -> Blocks.CYAN_WOOL;
            case 10 -> Blocks.PURPLE_WOOL;
            case 11 -> Blocks.BLUE_WOOL;
            case 12 -> Blocks.BROWN_WOOL;
            case 13 -> Blocks.GREEN_WOOL;
            case 14 -> Blocks.RED_WOOL;
            case 15 -> Blocks.BLACK_WOOL;
            default -> Blocks.WHITE_WOOL;
        };
    }

    private static ItemLike legacyDye(int damage)
    {
        return switch (damage)
        {
            case 1 -> Items.RED_DYE;
            case 2 -> Items.GREEN_DYE;
            case 3 -> Items.COCOA_BEANS;
            case 4 -> Items.LAPIS_LAZULI;
            case 5 -> Items.PURPLE_DYE;
            case 6 -> Items.CYAN_DYE;
            case 7 -> Items.LIGHT_GRAY_DYE;
            case 8 -> Items.GRAY_DYE;
            case 9 -> Items.PINK_DYE;
            case 10 -> Items.LIME_DYE;
            case 11 -> Items.YELLOW_DYE;
            case 12 -> Items.LIGHT_BLUE_DYE;
            case 13 -> Items.MAGENTA_DYE;
            case 14 -> Items.ORANGE_DYE;
            case 15 -> Items.BONE_MEAL;
            default -> Items.INK_SAC;
        };
    }
}
