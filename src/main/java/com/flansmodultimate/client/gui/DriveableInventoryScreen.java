package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu.Page;
import com.flansmodultimate.util.InventoryHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 1.7.10-style driveable hub, inventory, fuel and repair interface. */
public final class DriveableInventoryScreen extends AbstractContainerScreen<DriveableInventoryMenu>
{
    private static final ResourceLocation MENU_TEXTURE = texture("plane_menu.png");
    private static final ResourceLocation INVENTORY_TEXTURE = texture("plane_inventory.png");
    private static final ResourceLocation FUEL_TEXTURE = texture("plane_fuel.png");
    private static final ResourceLocation REPAIR_TEXTURE = texture("repair.png");

    private static final int LEGACY_WIDTH = 176;
    private static final int LEGACY_HEIGHT = 180;
    private static final int LEGACY_X_OFFSET = 13;
    private static final int REPAIR_ROWS = 3;

    private final Map<Page, Button> pageButtons = new EnumMap<>(Page.class);
    private final Button[] repairButtons = new Button[REPAIR_ROWS];
    private int repairOffset;

    public DriveableInventoryScreen(DriveableInventoryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = 202;
        imageHeight = LEGACY_HEIGHT;
        inventoryLabelX = LEGACY_X_OFFSET + 8;
        inventoryLabelY = 86;
    }

    private static ResourceLocation texture(String name)
    {
        return ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/driveable/" + name);
    }

    private int legacyLeft()
    {
        return leftPos + LEGACY_X_OFFSET;
    }

    @Override
    protected void init()
    {
        super.init();
        int x = legacyLeft() + 28;
        int y = topPos + 19;
        addPageButton(Page.CARGO, x, y, "Cargo");
        addPageButton(Page.GUNS, x + 62, y, "Guns");
        addPageButton(Page.FUEL, x, y + 22, "Fuel");
        addPageButton(Page.MISSILES, x + 62, y + 22, "Missiles");
        addPageButton(Page.REPAIR, x, y + 44, "Repair");
        addPageButton(Page.BOMBS, x + 62, y + 44, "Bombs");

        // Mecha addons did not use the plane menu in 1.7.10. Keep them
        // reachable without changing the six-button vehicle layout.
        addPageButton(Page.ADDONS, legacyLeft() + LEGACY_WIDTH + 4, y, "Addons");

        for (int row = 0; row < REPAIR_ROWS; row++)
        {
            int capturedRow = row;
            repairButtons[row] = addRenderableWidget(Button.builder(
                    Component.translatable("gui.flansmodultimate.driveable.repair"),
                    ignored -> repairVisiblePart(capturedRow))
                .bounds(leftPos + 9, topPos + 23, 45, 20).build());
        }
        refreshButtons();
    }

    private void addPageButton(Page page, int x, int y, String label)
    {
        Button button = Button.builder(Component.literal(label), ignored -> selectPage(page))
            .bounds(x, y, 58, 20).build();
        pageButtons.put(page, addRenderableWidget(button));
    }

    private void selectPage(Page page)
    {
        if (!menu.hasPage(page))
            return;
        sendMenuButton(DriveableInventoryMenu.PAGE_BUTTON_BASE + page.ordinal());
        repairOffset = 0;
        refreshButtons();
    }

    private void sendMenuButton(int id)
    {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null)
            return;
        if (menu.clickMenuButton(minecraft.player, id))
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        refreshButtons();
    }

    private void refreshButtons()
    {
        boolean hub = menu.getPage() == Page.MENU;
        for (Map.Entry<Page, Button> entry : pageButtons.entrySet())
        {
            boolean addons = entry.getKey() == Page.ADDONS;
            entry.getValue().visible = hub && (!addons || menu.hasPage(Page.ADDONS));
            entry.getValue().active = menu.hasPage(entry.getKey());
        }

        List<DriveablePart> parts = menu.getRepairParts();
        int y = topPos + 23;
        for (int row = 0; row < repairButtons.length; row++)
        {
            int index = repairOffset + row;
            Button button = repairButtons[row];
            if (menu.getPage() != Page.REPAIR || index >= parts.size())
            {
                button.visible = false;
                continue;
            }
            DriveablePart part = parts.get(index);
            button.setX(leftPos + 9);
            button.setY(y);
            button.visible = part.isDestroyed();
            button.active = button.visible && canAffordRepair(part);
            y += part.isDestroyed() ? 40 : 20;
        }
    }

    private boolean canAffordRepair(DriveablePart part)
    {
        if (minecraft == null || minecraft.player == null || menu.getDriveable() == null)
            return false;
        if (minecraft.player.getAbilities().instabuild)
            return true;
        return InventoryHelper.canConsumeAll(minecraft.player.getInventory(), menu.getDriveable().getConfigType()
            .getItemsRequired(part, menu.getDriveable().getDriveableData().getEngine()));
    }

    private void repairVisiblePart(int row)
    {
        List<DriveablePart> parts = menu.getRepairParts();
        int index = repairOffset + row;
        if (index >= 0 && index < parts.size() && parts.get(index).isDestroyed())
            sendMenuButton(DriveableInventoryMenu.REPAIR_BUTTON_BASE + parts.get(index).getType().ordinal());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        int x = legacyLeft();
        int y = topPos;
        switch (menu.getPage())
        {
            case MENU -> graphics.blit(MENU_TEXTURE, x, y, 0, 0, LEGACY_WIDTH, LEGACY_HEIGHT);
            case FUEL -> renderFuel(graphics, x, y);
            case REPAIR -> renderRepair(graphics);
            default -> renderInventoryPage(graphics, x, y);
        }
    }

    private void renderInventoryPage(GuiGraphics graphics, int x, int y)
    {
        graphics.blit(INVENTORY_TEXTURE, x, y, 0, 0, LEGACY_WIDTH, LEGACY_HEIGHT);
        int count = activeItemCount();
        if (menu.getPage() == Page.GUNS)
        {
            int visible = Math.max(0, count - menu.getScrollRow());
            for (int row = 0; row < Math.min(REPAIR_ROWS, visible); row++)
                graphics.blit(INVENTORY_TEXTURE, x + 9, y + 24 + row * 19, 176, 0, 37, 18);
        }
        else
        {
            int rows = Math.min(REPAIR_ROWS, (count + 7) / 8);
            for (int row = 0; row < rows; row++)
            {
                int remaining = count - (menu.getScrollRow() + row) * 8;
                int columns = Math.min(8, Math.max(0, remaining));
                if (columns > 0)
                    graphics.blit(INVENTORY_TEXTURE, x + 9, y + 24 + row * 19, 7, 97, columns * 18, 18);
            }
        }
        if (menu.getScrollRow() == 0)
            graphics.blit(INVENTORY_TEXTURE, x + 161, y + 41, 176, 18, 10, 10);
        if (menu.getScrollRow() == menu.getMaxScrollRow())
            graphics.blit(INVENTORY_TEXTURE, x + 161, y + 53, 176, 28, 10, 10);
    }

    private int activeItemCount()
    {
        if (menu.getDriveable() == null || menu.getDriveable().getDriveableData() == null)
            return 0;
        var data = menu.getDriveable().getDriveableData();
        return switch (menu.getPage())
        {
            case GUNS -> data.getNumAmmoSlots();
            case BOMBS -> data.getNumBombSlots();
            case MISSILES -> data.getNumMissileSlots();
            case CARGO -> data.getNumCargoSlots();
            case ADDONS -> data.getNumMechaSlots();
            default -> 0;
        };
    }

    private void renderFuel(GuiGraphics graphics, int x, int y)
    {
        y += 19;
        graphics.blit(FUEL_TEXTURE, x, y, 0, 0, LEGACY_WIDTH, 161);
        if (menu.getDriveable() == null || menu.getDriveable().getConfigType() == null)
            return;
        float capacity = menu.getDriveable().getConfigType().getFuelTankSize();
        float fraction = capacity <= 0F ? 0F : Mth.clamp(menu.getDriveable().getFuel() / capacity, 0F, 1F);
        int width = Math.round(129F * fraction);
        if (width > 0)
            graphics.blit(FUEL_TEXTURE, x + 26, y + 21, 0, 161, width, 15);
    }

    private void renderRepair(GuiGraphics graphics)
    {
        List<DriveablePart> parts = menu.getRepairParts();
        graphics.blit(REPAIR_TEXTURE, leftPos, topPos, 0, 0, 202, 23);
        int y = 23;
        for (int row = 0; row < REPAIR_ROWS; row++)
        {
            int index = repairOffset + row;
            if (index >= parts.size())
                break;
            DriveablePart part = parts.get(index);
            boolean broken = part.isDestroyed();
            int height = broken ? 40 : 20;
            graphics.blit(REPAIR_TEXTURE, leftPos, topPos + y, 0, 24, 202, height);

            float health = part.getMaxHealth() <= 0F ? 0F : Mth.clamp(part.getHealth() / part.getMaxHealth(), 0F, 1F);
            graphics.setColor(1F - health, health, 0F, 1F);
            graphics.blit(REPAIR_TEXTURE, leftPos + 111, topPos + y + 2, 0, 73, Math.round(70F * health), 16);
            graphics.setColor(1F, 1F, 1F, 1F);

            int nameX = broken ? 60 : 10;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(part.getType().getName(), broken ? 48 : 95)),
                leftPos + nameX, topPos + y + 6, 0xFFFFFF, false);
            graphics.drawCenteredString(font, Math.round(health * 100F) + "%", leftPos + 148, topPos + y + 6, 0xFFFFFF);

            if (broken && menu.getDriveable() != null)
            {
                List<ItemStack> required = menu.getDriveable().getConfigType().getItemsRequired(part,
                    menu.getDriveable().getDriveableData().getEngine());
                for (int item = 0; item < Math.min(7, required.size()); item++)
                {
                    int itemX = leftPos + 57 + item * 18;
                    int itemY = topPos + y + 22;
                    graphics.renderItem(required.get(item), itemX, itemY);
                    graphics.renderItemDecorations(font, required.get(item), itemX, itemY);
                }
            }
            y += height;
        }
        graphics.blit(REPAIR_TEXTURE, leftPos, topPos + y, 0, 65, 202, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        String vehicleName = menu.getDriveable() == null || menu.getDriveable().getConfigType() == null
            ? title.getString() : menu.getDriveable().getConfigType().getName();
        if (menu.getPage() == Page.REPAIR)
        {
            graphics.drawString(font, vehicleName + " - Repair", 7, 7, 0xFFFFFF, false);
            return;
        }

        String suffix = menu.getPage() == Page.MENU ? "" : " - " + menu.getPage().getDisplayName();
        int titleY = menu.getPage() == Page.FUEL ? 25 : 6;
        graphics.drawString(font, Component.literal(font.plainSubstrByWidth(vehicleName + suffix, 155)),
            LEGACY_X_OFFSET + 6, titleY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int x = legacyLeft();
        int y = topPos;
        int backY = y + (menu.getPage() == Page.FUEL ? 24 : 5);
        if (menu.getPage() != Page.MENU && menu.getPage() != Page.REPAIR
            && mouseX > x + 161 && mouseX < x + 171 && mouseY > backY && mouseY < backY + 10)
        {
            selectPage(Page.MENU);
            return true;
        }
        if (menu.getPage() != Page.MENU && menu.getPage() != Page.FUEL && menu.getPage() != Page.REPAIR)
        {
            if (mouseX > x + 161 && mouseX < x + 171 && mouseY > y + 41 && mouseY < y + 51
                && menu.getScrollRow() > 0)
            {
                sendMenuButton(DriveableInventoryMenu.SCROLL_UP_BUTTON);
                return true;
            }
            if (mouseX > x + 161 && mouseX < x + 171 && mouseY > y + 53 && mouseY < y + 63
                && menu.getScrollRow() < menu.getMaxScrollRow())
            {
                sendMenuButton(DriveableInventoryMenu.SCROLL_DOWN_BUTTON);
                return true;
            }
        }
        if (menu.getPage() == Page.REPAIR && mouseX > leftPos + 185 && mouseX < leftPos + 195
            && mouseY > topPos + 5 && mouseY < topPos + 15)
        {
            selectPage(Page.MENU);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (menu.getPage() == Page.REPAIR)
        {
            int max = Math.max(0, menu.getRepairParts().size() - REPAIR_ROWS);
            repairOffset = Mth.clamp(repairOffset + (delta < 0D ? 1 : -1), 0, max);
            refreshButtons();
            return true;
        }
        if (menu.getPage() != Page.MENU && menu.getPage() != Page.FUEL)
        {
            int id = delta < 0D ? DriveableInventoryMenu.SCROLL_DOWN_BUTTON : DriveableInventoryMenu.SCROLL_UP_BUTTON;
            sendMenuButton(id);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
