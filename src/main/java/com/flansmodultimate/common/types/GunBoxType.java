package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.recipe.RecipeIngredient;
import com.flansmodultimate.common.recipe.RecipeParser;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.logError;
import static com.flansmodultimate.util.TypeReaderUtils.readValue;

public class GunBoxType extends BlockType
{
    private static final int MAX_GUNS_PER_PAGE = 8;
    private static final String DEFAULT_PAGE_NAME = "Default";

    @Getter
    protected final List<GunBoxPage> gunPages = new ArrayList<>();
    protected GunBoxPage currentPage = new GunBoxPage(DEFAULT_PAGE_NAME);
    protected GunBoxEntry currentGunEntry;

    /** Custom GUI variables. Use an unsigned hex code for colors.*/
    protected String guiTexturePath;
    @Getter
    protected String gunBoxTextColor = "404040";
    @Getter
    protected String itemListTextColor = "404040";
    @Getter
    protected String itemTextColor= "404040";
    @Getter
    protected String pageTextColor = "FFFFFF";
    @Getter
    protected String buttonTextColor = "FFFFFF";
    @Getter
    protected String buttonTextHoverColor = "FFFFA0";

    public GunBoxType()
    {
        gunPages.add(currentPage);
    }

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        guiTexturePath = readFileNameResource("GuiTexture", guiTexturePath, file);
        gunBoxTextColor = readValue("GunBoxNameColor", gunBoxTextColor, file);
        pageTextColor = readValue("PageTextColor", pageTextColor, file);
        itemListTextColor = readValue("ListTextColor", itemListTextColor, file);
        itemTextColor = readValue("ItemTextColor", itemTextColor, file);
        buttonTextColor = readValue("ButtonTextColor", buttonTextColor, file);
        buttonTextHoverColor = readValue("ButtonTextHighlight", buttonTextHoverColor, file);
    }

    @Override
    protected void readLine(String[] split, int lineIndex, TypeFile file)
    {
        super.readLine(split, lineIndex, file);

        if (split.length < 1)
            return;

        String key = split[0];

        if (key.equalsIgnoreCase("Page") || key.equalsIgnoreCase("SetPage"))
        {
            try
            {
                String pageName = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                setPage(StringUtils.defaultIfBlank(pageName, DEFAULT_PAGE_NAME));
            }
            catch (Exception ex)
            {
                logError("Setting Page name failed", file, ex);
            }
        }

        if (key.equalsIgnoreCase("AddGun"))
        {
            try
            {
                if (split.length < 2)
                {
                    logError("AddGun is missing an item shortName", file);
                    return;
                }

                if (currentPage.getRawEntryCount() >= MAX_GUNS_PER_PAGE)
                    addPage(DEFAULT_PAGE_NAME + " " + (gunPages.size() + 1));

                currentGunEntry = new GunBoxEntry(split[1],
                    RecipeParser.parseAmountThenItemReferences(split, 2, contentPack, file, "GunBox recipe " + String.join(" ", split)),
                    contentPack,
                    file,
                    String.join(" ", split),
                    false);
                currentPage.addEntry(currentGunEntry);
            }
            catch(Exception ex)
            {
                logError("Adding gun to GunBox failed", file, ex);
            }
        }

        if (key.equalsIgnoreCase("AddAmmo") || key.equalsIgnoreCase("AddAltAmmo") || key.equalsIgnoreCase("AddAlternateAmmo"))
        {
            try
            {
                if (split.length < 2)
                {
                    logError("AddAmmo is missing an item shortName", file);
                    return;
                }

                if (currentGunEntry == null)
                {
                    logError("AddAmmo appeared before any AddGun, skipping entry: " + String.join(" ", split), file);
                    return;
                }

                currentGunEntry.addAmmoEntry(new GunBoxEntry(split[1],
                    RecipeParser.parseAmountThenItemReferences(split, 2, contentPack, file, "GunBox recipe " + String.join(" ", split)),
                    contentPack,
                    file,
                    String.join(" ", split),
                    true));
            }
            catch (Exception ex)
            {
                logError("Adding ammo to GunBox failed", file, ex);
            }
        }
    }

    protected void setPage(String pageName)
    {
        if (!currentPage.hasRawEntries())
        {
            currentPage.setPageName(pageName);
            return;
        }

        addPage(pageName);
    }

    protected void addPage(String pageName)
    {
        currentPage = new GunBoxPage(pageName);
        currentGunEntry = null;
        gunPages.add(currentPage);
    }

    public void resolveDeferredReferences()
    {
        for (GunBoxPage page : gunPages)
            page.resolveDeferredReferences();
    }

    public GunBoxEntry findEntry(InfoType type)
    {
        if (type == null)
            return null;

        for (GunBoxPage page : gunPages)
        {
            for (GunBoxEntry entry : page.getRawEntries())
            {
                if (entry.getType() == type)
                    return entry;

                for (GunBoxEntry ammoEntry : entry.getRawAmmoEntryList())
                {
                    if (ammoEntry.getType() == type)
                        return ammoEntry;
                }
            }
        }
        return null;
    }

    public ResourceLocation getGuiTexture()
    {
        return loadGuiTextureLocation(guiTexturePath, FlansMod.gunBoxGuiTexture);
    }

    public static class GunBoxPage
    {
        @Setter
        @Getter
        protected String pageName;
        protected final List<GunBoxEntry> entries = new ArrayList<>();

        public GunBoxPage(String pageName)
        {
            this.pageName = pageName;
        }

        public List<GunBoxEntry> getEntries()
        {
            return entries.stream().filter(GunBoxEntry::isResolved).toList();
        }

        protected List<GunBoxEntry> getRawEntries()
        {
            return entries;
        }

        protected int getRawEntryCount()
        {
            return entries.size();
        }

        protected boolean hasRawEntries()
        {
            return !entries.isEmpty();
        }

        protected void addEntry(GunBoxEntry entry)
        {
            entries.add(entry);
        }

        protected void resolveDeferredReferences()
        {
            entries.forEach(GunBoxEntry::resolveDeferredReferences);
        }
    }

    public static class GunBoxEntry
    {
        @Getter
        final String itemShortName;
        final List<RecipeIngredient> requiredPartRefs = new ArrayList<>();
        final List<GunBoxEntry> ammoEntryList = new ArrayList<>();
        @Nullable
        final IContentProvider contentPack;
        final TypeFile sourceFile;
        final String sourceLine;
        final boolean ammoEntry;
        @Nullable
        InfoType type;
        boolean typeResolved;
        boolean missingTypeLogged;

        public GunBoxEntry(String itemShortName, List<RecipeIngredient> parts, @Nullable IContentProvider contentPack, TypeFile sourceFile, String sourceLine, boolean ammoEntry)
        {
            this.itemShortName = itemShortName;
            this.contentPack = contentPack;
            this.sourceFile = sourceFile;
            this.sourceLine = sourceLine;
            this.ammoEntry = ammoEntry;
            requiredPartRefs.addAll(parts);
        }

        @Nullable
        public InfoType getType()
        {
            if (!typeResolved)
            {
                type = InfoType.getInfoType(itemShortName, contentPack);
                typeResolved = true;
            }

            if (type == null && !missingTypeLogged)
            {
                String message = ammoEntry
                    ? "AddAmmo item '" + itemShortName + "' not found for GunBox, skipping entry: " + sourceLine
                    : "Unable to find item '" + itemShortName + "' for GunBox, skipping entry: " + sourceLine;
                logError(message, sourceFile);
                missingTypeLogged = true;
            }

            return type;
        }

        public boolean isResolved()
        {
            return getType() != null;
        }

        public List<GunBoxEntry> getAmmoEntryList()
        {
            return ammoEntryList.stream().filter(GunBoxEntry::isResolved).toList();
        }

        protected List<GunBoxEntry> getRawAmmoEntryList()
        {
            return ammoEntryList;
        }

        protected void addAmmoEntry(GunBoxEntry entry)
        {
            ammoEntryList.add(entry);
        }

        protected void resolveDeferredReferences()
        {
            getType();
            ammoEntryList.forEach(GunBoxEntry::resolveDeferredReferences);
        }

        public List<ItemStack> getRequiredParts()
        {
            List<ItemStack> requiredParts = new ArrayList<>();
            for (RecipeIngredient recipeItem : requiredPartRefs)
            {
                ItemStack stack = recipeItem.resolve();
                if (!stack.isEmpty())
                    requiredParts.add(stack);
            }
            return requiredParts;
        }

        public boolean hasAmmoEntries()
        {
            return !getAmmoEntryList().isEmpty();
        }
    }
}
