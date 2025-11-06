package com.wolffsarmormod.common.paintjob;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LegacyDyeMapper
{
    public static ItemStack toDyeStack(String legacyName, int count)
    {
        return new ItemStack(toDyeItem(legacyName), count);
    }

    public static Item toDyeItem(String legacyName) {
        String k = normalize(legacyName);

        return switch (k)
        {
            case "black" -> Items.BLACK_DYE;
            case "red" -> Items.RED_DYE;
            case "green" -> Items.GREEN_DYE;
            case "brown" -> Items.BROWN_DYE;
            case "blue" -> Items.BLUE_DYE;
            case "purple" -> Items.PURPLE_DYE;
            case "cyan" -> Items.CYAN_DYE;

            // "silver" in old enums == light gray
            case "silver", "lightgray", "lightgrey" -> Items.LIGHT_GRAY_DYE;
            case "gray", "grey" -> Items.GRAY_DYE;

            // old "lightBlue" (camelCase) -> modern LIGHT_BLUE
            case "lightblue" -> Items.LIGHT_BLUE_DYE;
            case "pink" -> Items.PINK_DYE;
            case "lime" -> Items.LIME_DYE;
            case "yellow" -> Items.YELLOW_DYE;
            case "magenta" -> Items.MAGENTA_DYE;
            case "orange" -> Items.ORANGE_DYE;
            case "white" -> Items.WHITE_DYE;
            default -> throw new IllegalArgumentException("Unknown legacy dye name: " + legacyName);
        };
    }

    private static String normalize(String s) {
        // Lowercase and drop underscores, dashes, and spaces; turn camelCase "lightBlue" into "lightblue".
        return s.toLowerCase(Locale.ROOT).replaceAll("[_\\-\\s]", "");
    }
}
