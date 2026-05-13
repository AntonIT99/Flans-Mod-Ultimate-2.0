package com.flansmodultimate.common.types;

import com.flansmodultimate.IContentProvider;
import lombok.Getter;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.flansmodultimate.util.TypeReaderUtils.logError;

public class ArmorBoxType extends BlockType
{
    @Getter
    protected List<ArmourBoxEntry> pages = new ArrayList<>();

    @Override
    protected void readLine(String[] split, int lineIndex, TypeFile file)
    {
        super.readLine(split, lineIndex, file);

        if (split.length > 0 && (split[0].equalsIgnoreCase("AddArmour") || split[0].equalsIgnoreCase("AddArmor")))
        {
            try
            {
                ArmourBoxEntry entry = createArmourBoxEntry(split);
                readArmourPieces(entry, file, lineIndex, contentPack);
                pages.add(entry);
            }
            catch (Exception ex)
            {
                logError("Adding armor to box failed", file, ex);
            }
        }
    }

    protected static ArmourBoxEntry createArmourBoxEntry(String[] split)
    {
        String name = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
        return new ArmourBoxEntry(split[1], name);
    }

    protected void readArmourPieces(ArmourBoxEntry entry, TypeFile file, int lineIndex, IContentProvider contentPack)
    {
        List<String> lines = file.getLines();
        int armorSlot = 0;
        int offset = 1;

        while (armorSlot < 4 && lineIndex + offset < lines.size())
        {

            String line = lines.get(lineIndex + offset);
            offset++;

            if (line == null || line.startsWith("//"))
                continue;

            parseArmourLine(entry, armorSlot, line, file, contentPack);
            armorSlot++;
        }
    }

    protected void parseArmourLine(ArmourBoxEntry entry, int armorSlot, String line, TypeFile file, IContentProvider contentPack)
    {
        String[] lineSplit = line.split("\\s+");
        entry.armors[armorSlot] = lineSplit[0].toLowerCase(Locale.ROOT);

        for (int j = 0; j < (lineSplit.length - 1) / 2; j++)
        {
            ItemStack recipeElement = parseRecipeElement(lineSplit, j, contentPack);

            if (recipeElement != null)
                entry.requiredStacks.get(armorSlot).add(recipeElement);
            else
                logError("Could not find item for armour recipe " + line, file);
        }
    }

    protected ItemStack parseRecipeElement(String[] lineSplit, int j, IContentProvider contentPack) {
        String itemToken = lineSplit[j * 2 + 1];
        int amount = Integer.parseInt(lineSplit[j * 2 + 2]);

        String itemName;
        int meta;

        int dotIndex = itemToken.indexOf('.');
        if (dotIndex >= 0)
        {
            itemName = itemToken.substring(0, dotIndex);
            meta = Integer.parseInt(itemToken.substring(dotIndex + 1));
        }
        else
        {
            itemName = itemToken;
            meta = 0;
        }

        return getRecipeElement(itemName, amount, meta, contentPack);
    }

    /** Each instance of this class refers to one page full of recipes, that is, one full set of armour */
    @Getter
    public static class ArmourBoxEntry
    {
        final String shortName;
        final String name;
        final String[] armors = new String[4];
        final List<List<ItemStack>> requiredStacks = new ArrayList<>(4);

        public ArmourBoxEntry(String s, String s1)
        {
            shortName = s;
            name = s1;
            for (int i = 0; i < 4; i++)
                requiredStacks.add(new ArrayList<>());
        }
    }
}
