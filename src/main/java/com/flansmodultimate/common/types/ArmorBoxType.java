package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.recipe.RecipeIngredient;
import com.flansmodultimate.common.recipe.RecipeParser;
import com.flansmodultimate.util.ResourceUtils;
import lombok.Getter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.logError;

public class ArmorBoxType extends BlockType
{
    @Getter
    protected List<ArmourBoxEntry> pages = new ArrayList<>();
    protected String guiTexturePath;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        guiTexturePath = readFileNameResource("GuiTexture", guiTexturePath, file);
    }

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
        String armorShortName = ResourceUtils.sanitize(lineSplit[0]);
        entry.armors[armorSlot] = ContentManager.getShortnameAliasInContentPack(armorShortName, contentPack);
        entry.requiredStackRefs.get(armorSlot).addAll(RecipeParser.parseItemThenAmountReferences(lineSplit, 1, contentPack, file, "armour recipe " + line));
    }

    public ResourceLocation getGuiTexture()
    {
        return loadGuiTextureLocation(guiTexturePath, FlansMod.armorBoxGuiTexture);
    }

    /** Each instance of this class refers to one page full of recipes, that is, one full set of armour */
    @Getter
    public static class ArmourBoxEntry
    {
        final String shortName;
        final String name;
        final String[] armors = new String[4];
        final List<List<RecipeIngredient>> requiredStackRefs = new ArrayList<>(4);

        public ArmourBoxEntry(String s, String s1)
        {
            shortName = s;
            name = s1;
            for (int i = 0; i < 4; i++)
                requiredStackRefs.add(new ArrayList<>());
        }

        public List<List<ItemStack>> getRequiredStacks()
        {
            List<List<ItemStack>> requiredStacks = new ArrayList<>(4);
            for (List<RecipeIngredient> recipeItems : requiredStackRefs)
            {
                List<ItemStack> stacks = new ArrayList<>();
                for (RecipeIngredient recipeItem : recipeItems)
                {
                    ItemStack stack = recipeItem.resolve();
                    if (!stack.isEmpty())
                        stacks.add(stack);
                }
                requiredStacks.add(stacks);
            }
            return requiredStacks;
        }
    }
}
