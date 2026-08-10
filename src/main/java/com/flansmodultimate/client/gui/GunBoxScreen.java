package com.flansmodultimate.client.gui;

import com.flansmodultimate.common.inventory.GunBoxMenu;
import com.flansmodultimate.common.types.GunBoxType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketBuyWeapon;
import com.flansmodultimate.util.ModUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class GunBoxScreen extends AbstractContainerScreen<GunBoxMenu>
{
    private static final int TEX_W = 512;
    private static final int TEX_H = 256;
    private static final int MAX_GUNS_VISIBLE = 8;
    private static final int MAX_AMMO_TABS_VISIBLE = 5;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 43;
    private static final int LIST_W = 108;
    private static final int LIST_ENTRY_H = 12;

    private static final int BACK_X = 7;
    private static final int NEXT_X = 97;
    private static final int PAGE_BUTTON_Y = 20;
    private static final int PAGE_BUTTON_SIZE = 20;

    private static final int WEAPON_TAB_X = 121;
    private static final int TAB_Y = 20;
    private static final int TAB_H = 25;
    private static final int AMMO_TAB_X = 154;
    private static final int AMMO_TAB_STEP = 22;

    private static final int CRAFT_X = 126;
    private static final int CRAFT_Y = 111;
    private static final int CRAFT_W = 64;
    private static final int CRAFT_H = 20;

    private int pageIndex;
    private int hoveredEntry = -1;
    private int selectedEntry = -1;
    private int selectedAmmoEntry = -1;
    private boolean tabToAmmo;
    private boolean craftHighlight;
    private boolean nextHighlight;
    private boolean backHighlight;
    private ItemStack tooltipStack = ItemStack.EMPTY;

    public GunBoxScreen(GunBoxMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        imageWidth = 273;
        imageHeight = 233;
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(gg, mouseX, mouseY, partialTick);
        updateHoverState(mouseX, mouseY);
        super.render(gg, mouseX, mouseY, partialTick);
        renderTooltip(gg, mouseX, mouseY);
        renderCustomTooltip(gg, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gg, float partialTick, int mouseX, int mouseY)
    {
        GunBoxType type = menu.getBlock().getConfigType();
        gg.blit(type.getGuiTexture(), leftPos, topPos, 0F, 0F, imageWidth, imageHeight, TEX_W, TEX_H);

        if (hoveredEntry != -1)
            gg.blit(type.getGuiTexture(), leftPos + LIST_X, topPos + LIST_Y + (hoveredEntry * LIST_ENTRY_H), 383F, 5F, LIST_W, LIST_ENTRY_H, TEX_W, TEX_H);

        GunBoxType.GunBoxEntry entry = getSelectedEntry().orElse(null);
        if (entry != null)
        {
            gg.blit(type.getGuiTexture(), leftPos + LIST_X, topPos + LIST_Y + (selectedEntry * LIST_ENTRY_H), 275F, 5F, LIST_W, LIST_ENTRY_H, TEX_W, TEX_H);
            gg.blit(type.getGuiTexture(), leftPos + 121, topPos + 20, 275F, 207F, 144, 25, TEX_W, TEX_H);

            if (tabToAmmo)
                gg.blit(type.getGuiTexture(), leftPos + 121, topPos + 45, 275F, 112F, 144, 95, TEX_W, TEX_H);
            else
            {
                gg.blit(type.getGuiTexture(), leftPos + 121, topPos + 45, 275F, 17F, 144, 95, TEX_W, TEX_H);
                gg.blit(type.getGuiTexture(), leftPos + 127, topPos + 26, 419F, 33F, 16, 16, TEX_W, TEX_H);
            }

            int ammoTabCount = Math.min(entry.getAmmoEntryList().size(), MAX_AMMO_TABS_VISIBLE);
            for (int i = 0; i < ammoTabCount; i++)
                gg.blit(type.getGuiTexture(), leftPos + AMMO_TAB_X + (i * AMMO_TAB_STEP), topPos + 25, 435F, 17F, 18, 18, TEX_W, TEX_H);

            if (tabToAmmo && selectedAmmoEntry >= 0 && selectedAmmoEntry < ammoTabCount)
                gg.blit(type.getGuiTexture(), leftPos + 155 + (selectedAmmoEntry * AMMO_TAB_STEP), topPos + 26, 419F, 17F, 16, 16, TEX_W, TEX_H);

            gg.blit(type.getGuiTexture(), leftPos + CRAFT_X, topPos + CRAFT_Y, 419F, craftHighlight ? 85F : 65F, CRAFT_W, CRAFT_H, TEX_W, TEX_H);
        }

        gg.blit(type.getGuiTexture(), leftPos + NEXT_X, topPos + PAGE_BUTTON_Y, nextHighlight ? 439F : 419F, 105F, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, TEX_W, TEX_H);
        gg.blit(type.getGuiTexture(), leftPos + BACK_X, topPos + PAGE_BUTTON_Y, backHighlight ? 439F : 419F, 105F, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE, TEX_W, TEX_H);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gg, int mouseX, int mouseY)
    {
        GunBoxType type = menu.getBlock().getConfigType();
        GunBoxType.GunBoxPage page = getCurrentPage();

        gg.drawString(font, menu.getBlock().getName().getString(), 7, 6, hexColor(type.getGunBoxTextColor()), false);
        if (page != null)
            gg.drawString(font, page.getPageName(), 62 - (font.width(page.getPageName()) / 2), 26, hexColor(type.getPageTextColor()), true);

        List<GunBoxType.GunBoxEntry> entries = page == null ? List.of() : page.getEntries();
        for (int i = 0; i < entries.size() && i < MAX_GUNS_VISIBLE; i++)
        {
            String label = getDisplayName(entries.get(i));
            label = font.plainSubstrByWidth(label, 97);
            gg.drawString(font, label, 19, 46 + (i * LIST_ENTRY_H), hexColor(type.getItemListTextColor()), false);
        }

        GunBoxType.GunBoxEntry selected = getSelectedEntry().orElse(null);
        if (selected != null)
            renderSelectedEntry(gg, type, selected);

        drawButtonText(gg, ">", 107, 26, nextHighlight ? type.getButtonTextHoverColor() : type.getButtonTextColor());
        drawButtonText(gg, "<", 17, 26, backHighlight ? type.getButtonTextHoverColor() : type.getButtonTextColor());
    }

    private void renderSelectedEntry(GuiGraphics gg, GunBoxType type, GunBoxType.GunBoxEntry entry)
    {
        renderInfoTypeStack(gg, entry, 127, 26);

        int ammoTabCount = Math.min(entry.getAmmoEntryList().size(), MAX_AMMO_TABS_VISIBLE);
        for (int i = 0; i < ammoTabCount; i++)
            renderInfoTypeStack(gg, entry.getAmmoEntryList().get(i), 155 + (i * AMMO_TAB_STEP), 26);

        GunBoxType.GunBoxEntry displayedEntry = getDisplayedCraftEntry().orElse(entry);
        gg.drawString(font, getDisplayName(displayedEntry), 127, 52, hexColor(type.getItemTextColor()), false);
        drawRecipe(gg, displayedEntry.getRequiredParts());
        drawButtonText(gg, "Craft", 158, 117, craftHighlight ? type.getButtonTextHoverColor() : type.getButtonTextColor());
    }

    private void drawRecipe(GuiGraphics gg, List<ItemStack> parts)
    {
        for (int i = 0; i < parts.size() && i < 8; i++)
        {
            ItemStack stack = parts.get(i);
            int x = i < 4 ? 127 + (i * 19) : 127 + ((i - 4) * 19);
            int y = i < 4 ? 68 : 87;
            gg.renderItem(stack, x, y);
            gg.renderItemDecorations(font, stack, x, y);
        }
    }

    private void renderInfoTypeStack(GuiGraphics gg, GunBoxType.GunBoxEntry entry, int x, int y)
    {
        ModUtils.getItemStack(entry.getType()).ifPresent(stack -> {
            gg.renderItem(stack, x, y);
            gg.renderItemDecorations(font, stack, x, y);
        });
    }

    private void drawButtonText(GuiGraphics gg, String text, int centerX, int y, String color)
    {
        gg.drawString(font, text, centerX - (font.width(text) / 2), y, hexColor(color), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 || button == 1)
        {
            updateHoverState((int) mouseX, (int) mouseY);

            if (hoveredEntry != -1)
            {
                selectedEntry = hoveredEntry;
                selectedAmmoEntry = -1;
                tabToAmmo = false;
                return true;
            }

            int m = (int) mouseX - leftPos;
            int n = (int) mouseY - topPos;

            if (m >= WEAPON_TAB_X && m <= WEAPON_TAB_X + 27 && n >= TAB_Y && n <= TAB_Y + TAB_H)
            {
                tabToAmmo = false;
                return true;
            }

            if (backHighlight && getPageCount() > 1)
            {
                pageIndex = pageIndex == 0 ? getPageCount() - 1 : pageIndex - 1;
                resetSelection();
                return true;
            }

            if (nextHighlight && getPageCount() > 1)
            {
                pageIndex = pageIndex == getPageCount() - 1 ? 0 : pageIndex + 1;
                resetSelection();
                return true;
            }

            GunBoxType.GunBoxEntry entry = getSelectedEntry().orElse(null);
            if (entry != null && entry.hasAmmoEntries())
            {
                int ammoTabCount = Math.min(entry.getAmmoEntryList().size(), MAX_AMMO_TABS_VISIBLE);
                for (int i = 0; i < ammoTabCount; i++)
                {
                    int tabX = AMMO_TAB_X + (i * AMMO_TAB_STEP);
                    if (m >= tabX - 2 && m <= tabX + 19 && n >= 23 && n <= 44)
                    {
                        tabToAmmo = true;
                        selectedAmmoEntry = i;
                        return true;
                    }
                }
            }

            if (craftHighlight)
            {
                getDisplayedCraftEntry()
                    .map(GunBoxType.GunBoxEntry::getType)
                    .ifPresent(type -> PacketHandler.sendToServer(new PacketBuyWeapon(menu.getPos(), type.getShortName())));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateHoverState(int mouseX, int mouseY)
    {
        int m = mouseX - leftPos;
        int n = mouseY - topPos;

        hoveredEntry = -1;
        tooltipStack = ItemStack.EMPTY;

        GunBoxType.GunBoxPage page = getCurrentPage();
        List<GunBoxType.GunBoxEntry> entries = page == null ? List.of() : page.getEntries();
        for (int i = 0; i < entries.size() && i < MAX_GUNS_VISIBLE; i++)
        {
            int y = LIST_Y + (i * LIST_ENTRY_H);
            if (m >= LIST_X && m < LIST_X + LIST_W && n >= y && n < y + LIST_ENTRY_H)
            {
                hoveredEntry = i;
                break;
            }
        }

        craftHighlight = m >= CRAFT_X && m < CRAFT_X + CRAFT_W && n >= CRAFT_Y && n < CRAFT_Y + CRAFT_H;
        nextHighlight = m >= NEXT_X && m < NEXT_X + PAGE_BUTTON_SIZE && n >= PAGE_BUTTON_Y && n < PAGE_BUTTON_Y + PAGE_BUTTON_SIZE;
        backHighlight = m >= BACK_X && m < BACK_X + PAGE_BUTTON_SIZE && n >= PAGE_BUTTON_Y && n < PAGE_BUTTON_Y + PAGE_BUTTON_SIZE;

        updateTooltipStack(m, n);
    }

    private void updateTooltipStack(int m, int n)
    {
        GunBoxType.GunBoxEntry selected = getSelectedEntry().orElse(null);
        if (selected == null)
            return;

        if (isInBox(m, n, 127, 26, 16, 16))
        {
            tooltipStack = ModUtils.getItemStack(selected.getType()).orElse(ItemStack.EMPTY);
            return;
        }

        int ammoTabCount = Math.min(selected.getAmmoEntryList().size(), MAX_AMMO_TABS_VISIBLE);
        for (int i = 0; i < ammoTabCount; i++)
        {
            if (isInBox(m, n, 155 + (i * AMMO_TAB_STEP), 26, 16, 16))
            {
                tooltipStack = ModUtils.getItemStack(selected.getAmmoEntryList().get(i).getType()).orElse(ItemStack.EMPTY);
                return;
            }
        }

        GunBoxType.GunBoxEntry displayed = getDisplayedCraftEntry().orElse(selected);
        for (int i = 0; i < displayed.getRequiredParts().size() && i < 8; i++)
        {
            int x = i < 4 ? 127 + (i * 19) : 127 + ((i - 4) * 19);
            int y = i < 4 ? 68 : 87;
            if (isInBox(m, n, x, y, 16, 16))
            {
                tooltipStack = displayed.getRequiredParts().get(i);
                return;
            }
        }
    }

    private void renderCustomTooltip(GuiGraphics gg, int mouseX, int mouseY)
    {
        if (!tooltipStack.isEmpty())
            gg.renderTooltip(font, tooltipStack, mouseX, mouseY);
    }

    private boolean isInBox(int mouseX, int mouseY, int x, int y, int w, int h)
    {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private Optional<GunBoxType.GunBoxEntry> getSelectedEntry()
    {
        GunBoxType.GunBoxPage page = getCurrentPage();
        if (page == null || selectedEntry < 0 || selectedEntry >= page.getEntries().size())
            return Optional.empty();
        return Optional.of(page.getEntries().get(selectedEntry));
    }

    private Optional<GunBoxType.GunBoxEntry> getDisplayedCraftEntry()
    {
        Optional<GunBoxType.GunBoxEntry> selected = getSelectedEntry();
        if (selected.isEmpty())
            return Optional.empty();

        GunBoxType.GunBoxEntry entry = selected.get();
        if (tabToAmmo && selectedAmmoEntry >= 0 && selectedAmmoEntry < entry.getAmmoEntryList().size())
            return Optional.of(entry.getAmmoEntryList().get(selectedAmmoEntry));
        return selected;
    }

    private GunBoxType.GunBoxPage getCurrentPage()
    {
        List<GunBoxType.GunBoxPage> pages = menu.getBlock().getConfigType().getGunPages();
        if (pages.isEmpty())
            return null;
        if (pageIndex < 0 || pageIndex >= pages.size())
            pageIndex = 0;
        return pages.get(pageIndex);
    }

    private int getPageCount()
    {
        return menu.getBlock().getConfigType().getGunPages().size();
    }

    private void resetSelection()
    {
        selectedEntry = -1;
        selectedAmmoEntry = -1;
        tabToAmmo = false;
    }

    private String getDisplayName(GunBoxType.GunBoxEntry entry)
    {
        InfoType entryType = entry.getType();
        return ModUtils.getItemStack(entryType)
            .map(stack -> stack.getHoverName().getString())
            .orElseGet(() -> entryType == null ? entry.getItemShortName() : entryType.getName());
    }

    private int hexColor(String color)
    {
        try
        {
            return Integer.parseUnsignedInt(color, 16);
        }
        catch (NumberFormatException ignored)
        {
            return 0xFFFFFF;
        }
    }
}
