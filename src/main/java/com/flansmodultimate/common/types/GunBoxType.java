package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.recipe.RecipeIngredient;
import com.flansmodultimate.common.recipe.RecipeParser;
import com.flansmodultimate.util.ResourceUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

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

                InfoType type = resolveInfoType(split[1]);
                if (type != null)
                {
                    if (currentPage.getEntries().size() >= MAX_GUNS_PER_PAGE)
                        addPage(DEFAULT_PAGE_NAME + " " + (gunPages.size() + 1));

                    currentGunEntry = new GunBoxEntry(type, RecipeParser.parseAmountThenItemReferences(split, 2, contentPack, file, "GunBox recipe " + String.join(" ", split)));
                    currentPage.getEntries().add(currentGunEntry);
                }
                else
                {
                    logError("Unable to find item '" + split[1] + "' for GunBox, skipping entry: " + String.join(" ", split), file);
                }
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

                InfoType addAmmoType = resolveInfoType(split[1]);
                if (addAmmoType == null)
                {
                    logError("AddAmmo item '" + split[1] + "' not found for GunBox, skipping entry: " + String.join(" ", split), file);
                    return;
                }

                if (currentGunEntry == null)
                {
                    logError("AddAmmo appeared before any AddGun, skipping entry: " + String.join(" ", split), file);
                    return;
                }

                currentGunEntry.getAmmoEntryList().add(new GunBoxEntry(addAmmoType, RecipeParser.parseAmountThenItemReferences(split, 2, contentPack, file, "GunBox recipe " + String.join(" ", split))));
            }
            catch (Exception ex)
            {
                logError("Adding ammo to GunBox failed", file, ex);
            }
        }
    }

    protected void setPage(String pageName)
    {
        if (currentPage.getEntries().isEmpty())
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

    protected InfoType resolveInfoType(String id)
    {
        String sanitizedId = ResourceUtils.sanitize(id);
        String aliasedId = ContentManager.getShortnameAliasInContentPack(sanitizedId, contentPack);
        InfoType type = InfoType.getInfoType(aliasedId);
        if (type == null)
            type = InfoType.getInfoType(sanitizedId);
        return type;
    }

    public GunBoxEntry findEntry(InfoType type)
    {
        if (type == null)
            return null;

        for (GunBoxPage page : gunPages)
        {
            for (GunBoxEntry entry : page.getEntries())
            {
                if (entry.getType() == type)
                    return entry;

                for (GunBoxEntry ammoEntry : entry.getAmmoEntryList())
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

    @Getter
    public static class GunBoxPage
    {
        protected String pageName;
        protected final List<GunBoxEntry> entries = new ArrayList<>();

        public GunBoxPage(String pageName)
        {
            this.pageName = pageName;
        }

        public void setPageName(String pageName)
        {
            this.pageName = pageName;
        }
    }

    @Getter
    public static class GunBoxEntry
    {
        final InfoType type;
        final List<RecipeIngredient> requiredPartRefs = new ArrayList<>();
        final List<GunBoxEntry> ammoEntryList = new ArrayList<>();

        public GunBoxEntry(InfoType aType, List<RecipeIngredient> aParts)
        {
            type = aType;
            requiredPartRefs.addAll(aParts);
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
            return !ammoEntryList.isEmpty();
        }
    }
}
