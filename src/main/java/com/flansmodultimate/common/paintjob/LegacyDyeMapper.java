package com.flansmodultimate.common.paintjob;

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
            case "black" -> Items.DYE.black();
            case "red" -> Items.DYE.red();
            case "green" -> Items.DYE.green();
            case "brown" -> Items.DYE.brown();
            case "blue" -> Items.DYE.blue();
            case "purple" -> Items.DYE.purple();
            case "cyan" -> Items.DYE.cyan();

            // "silver" in old enums == light gray
            case "silver", "lightgray", "lightgrey" -> Items.DYE.lightGray();
            case "gray", "grey" -> Items.DYE.gray();

            // old "lightBlue" (camelCase) -> modern LIGHT_BLUE
            case "lightblue" -> Items.DYE.lightBlue();
            case "pink" -> Items.DYE.pink();
            case "lime" -> Items.DYE.lime();
            case "yellow" -> Items.DYE.yellow();
            case "magenta" -> Items.DYE.magenta();
            case "orange" -> Items.DYE.orange();
            case "white" -> Items.DYE.white();
            default -> throw new IllegalArgumentException("Unknown legacy dye name: " + legacyName);
        };
    }

    private static String normalize(String s) {
        // Lowercase and drop underscores, dashes, and spaces; turn camelCase "lightBlue" into "lightblue".
        return s.toLowerCase(Locale.ROOT).replaceAll("[_\\-\\s]", "");
    }
}
