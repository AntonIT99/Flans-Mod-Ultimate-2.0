package com.flansmodultimate.common.types;

import lombok.Getter;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.flansmodultimate.util.TypeReaderUtils.logError;
import static com.flansmodultimate.util.TypeReaderUtils.readValue;

public class GunBoxType extends BlockType
{
    protected Map<String, List<GunBoxEntry>> gunPages = new LinkedHashMap<>(Map.of("Default", new ArrayList<>()));
    protected List<GunBoxEntry> currentGunEntries = new ArrayList<>();
    protected String currentPageName = "Default";

    /** Custom GUI variables. Use an unsigned hex code for colors.*/
    protected String guiTexturePath;
    protected String gunBoxTextColor = "404040";
    protected String itemListTextColor = "404040";
    protected String itemTextColor= "404040";
    protected String pageTextColor = "FFFFFF";
    protected String buttonTextColor = "FFFFFF";
    protected String buttonTextHoverColor = "FFFFA0";

    @Override
    protected void read(TypeFile file)
    {
        guiTexturePath = readResource("GuiTexture", guiTexturePath, file);
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
        if (split.length > 0 && (split[0].equalsIgnoreCase("Page") || split[0].equalsIgnoreCase("SetPage")))
        {
            try
            {
                String pageName = String.join(" ", Arrays.copyOfRange(split, 1, split.length));

                //If empty, rename the page. If not, add the current page to list and start next one.
                if (!currentGunEntries.isEmpty())
                {
                    gunPages.get(currentPageName).addAll(currentGunEntries);
                    currentGunEntries.clear();
                }
                else
                {
                    gunPages.remove(currentPageName);
                }

                currentPageName = pageName;
                gunPages.putIfAbsent(currentPageName, new ArrayList<>());
            }
            catch (Exception ex)
            {
                logError("Setting Page name failed", file, ex);
            }
        }

        //TODO: Fix this code
        /*if (split.length > 0 && split[0].equalsIgnoreCase("AddGun"))
        {
            try
            {
                InfoType type = InfoType.getInfoType(split[1]);
                if (type != null)
                {
                    List<ItemStack> parts = getRecipe(split);
                    nextGun++;
                    if(nextGun > gunEntries.length - 1)
                    {
                        gunPages.get(currentPageName).add(Arrays.copyOf(gunEntries, nextGun));
                        iteratePage("Default " + (gunPages.size() + 2));
                        nextGun++;
                    }
                    gunEntries[nextGun] = new GunBoxEntry(type, parts);
                }
                else
                {
                    logError("Unable to find item for GunBox, skipping entry", file);
                }
            }
            catch(Exception ex)
            {
                logError("Adding gun to GunBox failed", file, ex);
            }
        }*/
    }

    @Getter
    public static class GunBoxEntry
    {
        final InfoType type;
        final List<ItemStack> requiredParts = new ArrayList<>();
        final List<GunBoxEntry> ammoEntryList = new ArrayList<>();

        public GunBoxEntry(InfoType aType, List<ItemStack> aParts)
        {
            type = aType;
            requiredParts.addAll(aParts);
        }
    }
}
