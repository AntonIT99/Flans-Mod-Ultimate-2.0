package com.flansmodultimate.client.gui;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu.Page;
import com.flansmodultimate.util.InventoryHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Combined cargo, ammunition, fuel, mecha-addons and repair interface. */
public final class DriveableInventoryScreen extends AbstractContainerScreen<DriveableInventoryMenu>
{
    private static final int REPAIR_ROWS = 3;

    private final List<Button> pageButtons = new ArrayList<>();
    private final Button[] repairButtons = new Button[REPAIR_ROWS];
    private Button scrollUpButton;
    private Button scrollDownButton;
    private int repairOffset;

    public DriveableInventoryScreen(DriveableInventoryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = 230;
        imageHeight = 185;
        inventoryLabelX = 8;
        inventoryLabelY = 92;
    }

    @Override
    protected void init()
    {
        super.init();
        for (Page page : Page.values())
        {
            String label = switch (page)
            {
                case GUNS -> "Gun";
                case BOMBS -> "Bomb";
                case MISSILES -> "Miss";
                case CARGO -> "Cargo";
                case FUEL -> "Fuel";
                case ADDONS -> "Add";
                case REPAIR -> "Fix";
            };
            Button button = Button.builder(Component.literal(label), ignored -> selectPage(page))
                .bounds(leftPos + 3 + page.ordinal() * 32, topPos - 22, 31, 20).build();
            pageButtons.add(addRenderableWidget(button));
        }

        scrollUpButton = addRenderableWidget(Button.builder(Component.literal("▲"), ignored -> scroll(-1))
            .bounds(leftPos + 156, topPos + 31, 20, 20).build());
        scrollDownButton = addRenderableWidget(Button.builder(Component.literal("▼"), ignored -> scroll(1))
            .bounds(leftPos + 156, topPos + 55, 20, 20).build());
        for (int row = 0; row < REPAIR_ROWS; row++)
        {
            int capturedRow = row;
            repairButtons[row] = addRenderableWidget(Button.builder(Component.translatable("gui.flansmodultimate.driveable.repair"),
                    ignored -> repairVisiblePart(capturedRow))
                .bounds(leftPos + 178, topPos + 28 + row * 20, 46, 18).build());
        }
        refreshButtons();
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        refreshButtons();
    }

    private void selectPage(Page page)
    {
        if (!menu.hasPage(page))
            return;
        sendMenuButton(DriveableInventoryMenu.PAGE_BUTTON_BASE + page.ordinal());
        repairOffset = 0;
        refreshButtons();
    }

    private void scroll(int direction)
    {
        if (menu.getPage() == Page.REPAIR)
        {
            int max = Math.max(0, menu.getRepairParts().size() - REPAIR_ROWS);
            repairOffset = Mth.clamp(repairOffset + direction, 0, max);
        }
        else
            sendMenuButton(direction < 0 ? DriveableInventoryMenu.SCROLL_UP_BUTTON : DriveableInventoryMenu.SCROLL_DOWN_BUTTON);
        refreshButtons();
    }

    private void repairVisiblePart(int row)
    {
        List<DriveablePart> parts = menu.getRepairParts();
        int index = repairOffset + row;
        if (index < 0 || index >= parts.size() || !parts.get(index).isDestroyed())
            return;
        sendMenuButton(DriveableInventoryMenu.REPAIR_BUTTON_BASE + parts.get(index).getType().ordinal());
    }

    private void sendMenuButton(int id)
    {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null)
            return;
        if (menu.clickMenuButton(minecraft.player, id))
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void refreshButtons()
    {
        for (int index = 0; index < pageButtons.size(); index++)
        {
            Page page = Page.values()[index];
            Button button = pageButtons.get(index);
            button.visible = menu.hasPage(page);
            button.active = page != menu.getPage();
        }

        boolean repair = menu.getPage() == Page.REPAIR;
        int maxScroll = repair ? Math.max(0, menu.getRepairParts().size() - REPAIR_ROWS) : menu.getMaxScrollRow();
        int offset = repair ? repairOffset : menu.getScrollRow();
        scrollUpButton.visible = maxScroll > 0;
        scrollDownButton.visible = maxScroll > 0;
        scrollUpButton.active = offset > 0;
        scrollDownButton.active = offset < maxScroll;

        List<DriveablePart> parts = menu.getRepairParts();
        for (int row = 0; row < repairButtons.length; row++)
        {
            int index = repairOffset + row;
            Button button = repairButtons[row];
            button.visible = repair && index < parts.size() && parts.get(index).isDestroyed();
            button.active = button.visible && canAffordRepair(parts.get(index));
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
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0181C22);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF313842);

        if (menu.getPage() != Page.REPAIR)
        {
            for (int index = 0; index < 24; index++)
            {
                if (!menu.getSlot(index).isActive())
                    continue;
                int x = leftPos + menu.getSlot(index).x - 1;
                int y = topPos + menu.getSlot(index).y - 1;
                graphics.fill(x, y, x + 18, y + 18, 0xFF20262E);
            }
        }

        for (int index = 24; index < menu.slots.size(); index++)
        {
            int x = leftPos + menu.getSlot(index).x - 1;
            int y = topPos + menu.getSlot(index).y - 1;
            graphics.fill(x, y, x + 18, y + 18, 0xFF20262E);
        }
        if (menu.getPage() == Page.REPAIR)
            renderRepairRows(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        String name = menu.getDriveable() == null || menu.getDriveable().getConfigType() == null
            ? title.getString() : menu.getDriveable().getConfigType().getName();
        graphics.drawString(font, Component.literal(font.plainSubstrByWidth(name + " - " + menu.getPage().getDisplayName(), 210)), 8, 8, 0xFFFFFF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xD6DCE3, false);
    }

    private void renderRepairRows(GuiGraphics graphics)
    {
        List<DriveablePart> parts = menu.getRepairParts();
        for (int row = 0; row < REPAIR_ROWS; row++)
        {
            int index = repairOffset + row;
            if (index >= parts.size())
                break;
            DriveablePart part = parts.get(index);
            int x = leftPos + 8;
            int y = topPos + 29 + row * 20;
            graphics.fill(x, y, leftPos + 224, y + 18, 0xFF20262E);
            float health = part.getMaxHealth() <= 0F ? 0F : Mth.clamp(part.getHealth() / part.getMaxHealth(), 0F, 1F);
            int colour = part.isDestroyed() ? 0xFF9D3539 : 0xFF3D8C55;
            graphics.fill(x + 1, y + 12, x + 1 + Math.round(160F * health), y + 16, colour);
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(part.getType().getName(), 105)), x + 3, y + 2, 0xFFFFFF, false);
            graphics.drawString(font, Component.literal(Math.round(health * 100F) + "%"), x + 111, y + 2, 0xC9D2DC, false);
        }
    }
}
