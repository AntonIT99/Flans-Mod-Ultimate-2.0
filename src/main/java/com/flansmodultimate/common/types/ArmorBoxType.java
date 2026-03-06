package com.flansmodultimate.common.types;

import lombok.Getter;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.readValuesInLines;

public class ArmorBoxType extends BlockType
{
    @Getter
    protected List<ArmourBoxEntry> pages = new ArrayList<>();

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        readValuesInLines("AddArmour", file).ifPresent(lines -> lines.forEach(line -> readLine(line, file)));
    }

    protected void readLine(String[] split, TypeFile file)
    {
        //TODO: implement
        /*try {
            StringBuilder name = new StringBuilder(split[2]);

            for(int i = 3; i < split.length; i++)
                name.append(" ").append(split[i]);
            ArmourBoxEntry entry = new ArmourBoxEntry(split[1], name.toString());
            //Read the next 4 lines for each armour piece
            for (int i = 0; i < 4; i++)
            {
                String line;
                line = file.readLine();
                if(line == null)
                    continue;
                if(line.startsWith("//"))
                {
                    i--;
                    continue;
                }
                String[] lineSplit = line.split(" ");

                ArmourType armourType = ArmourType.getArmourType(lineSplit[0]);

                if (armourType != null) {
                    entry.armours[i] = armourType;
                    for(int j = 0; j < (lineSplit.length - 1) / 2; j++)
                    {
                        ItemStack recipeElement = null;
                        if(lineSplit[j * 2 + 1].contains("."))
                            recipeElement = getRecipeElement(lineSplit[j * 2 + 1].split("\\.")[0], Integer.parseInt(lineSplit[j * 2 + 2]), Integer.parseInt(lineSplit[j * 2 + 1].split("\\.")[1]), shortName);
                        else
                            recipeElement = getRecipeElement(lineSplit[j * 2 + 1], Integer.parseInt(lineSplit[j * 2 + 2]), 0, shortName);

                        if(recipeElement != null) {
                            entry.requiredStacks[i].add(recipeElement);
                        } else {
                            FlansMod.logPackError(file.name, packName, shortName, "Could not find item for armour recipe", split, null);
                        }
                    }
                } else {
                    FlansMod.logPackError(file.name, packName, shortName, "Couldn't find armour type for armour box", lineSplit, null);
                }
            }
        }
        catch (Exception ex)
        {
            FlansMod.logPackError(file.name, packName, shortName, "Adding armour to box failed", split, ex);
        }*/
    }

    /** Each instance of this class refers to one page full of recipes, that is, one full set of armour */
    @Getter
    public class ArmourBoxEntry
    {
        final String shortName;
        final String name;
        final ArmorType[] armours = new ArmorType[4];
        final List<ItemStack>[] requiredStacks = new List[4];

        public ArmourBoxEntry(String s, String s1)
        {
            shortName = s;
            name = s1;
            for (int i = 0; i < 4; i++)
                requiredStacks[i] = new ArrayList<>();
        }
    }
}
