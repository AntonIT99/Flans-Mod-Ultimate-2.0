package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.recipe.RecipeIngredient;
import com.flansmodultimate.common.recipe.RecipeParser;
import com.flansmodultimate.util.ResourceUtils;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

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
                ArmourBoxEntry entry = createArmourBoxEntry(split, contentPack);
                readArmourPieces(entry, file, lineIndex, contentPack);
                pages.add(entry);
            }
            catch (Exception ex)
            {
                logError("Adding armor to box failed", file, ex);
            }
        }
    }

    protected static ArmourBoxEntry createArmourBoxEntry(String[] split, @Nullable IContentProvider contentPack)
    {
        String name = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
        return new ArmourBoxEntry(split[1], name, contentPack);
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
        entry.setArmor(armorSlot, armorShortName, file, line);
        entry.requiredStackRefs.get(armorSlot).addAll(RecipeParser.parseItemThenAmountReferences(lineSplit, 1, contentPack, file, "armour recipe " + line));
    }

    public void resolveDeferredReferences()
    {
        pages.forEach(ArmourBoxEntry::resolveDeferredReferences);
    }

    public ResourceLocation getGuiTexture()
    {
        return loadGuiTextureLocation(guiTexturePath, FlansMod.armorBoxGuiTexture);
    }

    /** Each instance of this class refers to one page full of recipes, that is, one full set of armour */
    public static class ArmourBoxEntry
    {
        @Getter
        final String shortName;
        @Getter
        final String name;
        final String[] armors = new String[4];
        final ArmorType[] resolvedArmors = new ArmorType[4];
        final boolean[] armorResolved = new boolean[4];
        final boolean[] missingArmorLogged = new boolean[4];
        final boolean[] requiredStacksResolved = new boolean[4];
        final TypeFile[] armorSourceFiles = new TypeFile[4];
        final String[] armorSourceLines = new String[4];
        final List<List<RecipeIngredient>> requiredStackRefs = new ArrayList<>(4);
        @Nullable
        final IContentProvider contentPack;

        public ArmourBoxEntry(String s, String s1, @Nullable IContentProvider contentPack)
        {
            shortName = s;
            name = s1;
            this.contentPack = contentPack;
            for (int i = 0; i < 4; i++)
                requiredStackRefs.add(new ArrayList<>());
        }

        public void setArmor(int armorSlot, String armorShortName, TypeFile file, String sourceLine)
        {
            if (armorSlot < 0 || armorSlot >= armors.length)
                return;

            armors[armorSlot] = armorShortName;
            armorSourceFiles[armorSlot] = file;
            armorSourceLines[armorSlot] = sourceLine;
            armorResolved[armorSlot] = false;
            resolvedArmors[armorSlot] = null;
            missingArmorLogged[armorSlot] = false;
            requiredStacksResolved[armorSlot] = false;
        }

        @Nullable
        public ArmorType getArmorType(int armorSlot)
        {
            if (armorSlot < 0 || armorSlot >= armors.length)
                return null;

            String armorShortName = armors[armorSlot];
            if (armorShortName == null || armorShortName.isBlank())
                return null;

            if (!armorResolved[armorSlot])
            {
                InfoType type = InfoType.getInfoType(armorShortName, contentPack);
                if (type instanceof ArmorType armorType)
                    resolvedArmors[armorSlot] = armorType;
                armorResolved[armorSlot] = true;
            }

            if (resolvedArmors[armorSlot] == null && !missingArmorLogged[armorSlot])
            {
                TypeFile sourceFile = armorSourceFiles[armorSlot];
                String sourceLine = armorSourceLines[armorSlot];
                if (sourceFile != null)
                    logError("Unable to find armor item '" + armorShortName + "' for ArmorBox, skipping entry: " + sourceLine, sourceFile);
                missingArmorLogged[armorSlot] = true;
            }

            return resolvedArmors[armorSlot];
        }

        protected void resolveDeferredReferences()
        {
            for (int i = 0; i < armors.length; i++)
                getArmorType(i);
        }

        public List<List<ItemStack>> getRequiredStacks()
        {
            List<List<ItemStack>> requiredStacks = new ArrayList<>(4);
            for (int armorSlot = 0; armorSlot < requiredStackRefs.size(); armorSlot++)
                requiredStacks.add(resolveRequiredStacks(armorSlot));
            return requiredStacks;
        }

        private List<ItemStack> resolveRequiredStacks(int armorSlot)
        {
            List<ItemStack> stacks = new ArrayList<>();
            if (armorSlot < 0 || armorSlot >= requiredStackRefs.size())
                return stacks;

            for (RecipeIngredient recipeItem : requiredStackRefs.get(armorSlot))
            {
                ItemStack stack = recipeItem.resolve();
                if (!stack.isEmpty())
                {
                    stacks.add(stack);
                    continue;
                }

                if (!requiredStacksResolved[armorSlot])
                    logMissingRequiredStack(armorSlot, recipeItem);
            }
            requiredStacksResolved[armorSlot] = true;
            return stacks;
        }

        private void logMissingRequiredStack(int armorSlot, RecipeIngredient recipeItem)
        {
            TypeFile sourceFile = armorSourceFiles[armorSlot];
            if (sourceFile == null)
                return;

            String armorShortName = armors[armorSlot];
            String sourceLine = armorSourceLines[armorSlot];
            logError("Could not resolve ArmorBox recipe ingredient '" + recipeItem.getItemName()
                + "' (amount " + recipeItem.getAmount()
                + ") for armor item '" + armorShortName
                + "', skipping ingredient. Source line: " + sourceLine, sourceFile);
        }
    }
}
