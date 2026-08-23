package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu.Page;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.util.InventoryHelper;
import com.flansmodultimate.util.ModUtils;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 1.7.10-style driveable hub, inventory, fuel and repair interface. */
public final class DriveableInventoryScreen extends AbstractContainerScreen<DriveableInventoryMenu>
{
    private static final Identifier MENU_TEXTURE = texture("plane_menu.png");
    private static final Identifier INVENTORY_TEXTURE = texture("plane_inventory.png");
    private static final Identifier FUEL_TEXTURE = texture("plane_fuel.png");
    private static final Identifier REPAIR_TEXTURE = texture("repair.png");

    private static final int LEGACY_WIDTH = 176;
    private static final int LEGACY_HEIGHT = 180;
    private static final int LEGACY_X_OFFSET = 13;
    private static final int VISIBLE_INVENTORY_ROWS = 3;
    private final Map<Page, Button> pageButtons = new EnumMap<>(Page.class);
    private final List<Button> repairButtons = new ArrayList<>();
    private int repairOffset;

    private record GunRow(String name, GunType type) {}

    public DriveableInventoryScreen(DriveableInventoryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title, 202, LEGACY_HEIGHT);
        inventoryLabelX = LEGACY_X_OFFSET + 8;
        inventoryLabelY = 86;
    }

    private static Identifier texture(String name)
    {
        return Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/driveable/" + name);
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
        addPageButton(Page.MISSILES, x + 62, y + 22, missilePageName());
        addPageButton(Page.REPAIR, x, y + 44, "Repair");
        addPageButton(Page.BOMBS, x + 62, y + 44, "Bombs");

        // Mecha addons did not use the plane menu in 1.7.10. Keep them
        // reachable without changing the six-button vehicle layout.
        addPageButton(Page.ADDONS, legacyLeft() + LEGACY_WIDTH + 4, y, "Addons");

        List<DriveablePart> repairParts = menu.getRepairParts();
        for (int index = 0; index < repairParts.size(); index++)
        {
            int capturedIndex = index;
            repairButtons.add(addRenderableWidget(Button.builder(
                    Component.translatable("gui.flansmodultimate.driveable.repair"),
                    ignored -> repairPart(capturedIndex))
                .bounds(repairLeft() + 9, repairTop() + 23, 45, 20).build()));
        }
        refreshButtons();
    }

    private void addPageButton(Page page, int x, int y, String label)
    {
        Button button = Button.builder(Component.literal(label), ignored -> selectPage(page))
            .bounds(x, y, 58, 20).build();
        pageButtons.put(page, addRenderableWidget(button));
    }

    private String missilePageName()
    {
        return menu.getDriveable() != null && menu.getDriveable().getConfigType() instanceof PlaneType
            ? "Missiles" : "Shells";
    }

    private void selectPage(Page page)
    {
        if (!menu.hasPage(page))
            return;
        sendMenuButton(DriveableInventoryMenu.PAGE_BUTTON_BASE + page.ordinal());
        repairOffset = 0;
        clearButtonFocus();
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

        for (Button button : repairButtons)
            button.visible = false;
        if (menu.getPage() != Page.REPAIR)
            return;

        List<DriveablePart> parts = menu.getRepairParts();
        int end = visibleRepairEnd(parts);
        int y = repairTop() + 23;
        for (int index = repairOffset; index < end; index++)
        {
            Button button = repairButtons.get(index);
            DriveablePart part = parts.get(index);
            button.setX(repairLeft() + 9);
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

    private void repairPart(int index)
    {
        List<DriveablePart> parts = menu.getRepairParts();
        if (index >= 0 && index < parts.size() && parts.get(index).isDestroyed())
            sendMenuButton(DriveableInventoryMenu.REPAIR_BUTTON_BASE + parts.get(index).getType().ordinal());
    }

    private void clearButtonFocus()
    {
        setFocused(null);
        pageButtons.values().forEach(button -> button.setFocused(false));
    }

    private int repairLeft()
    {
        return (width - 202) / 2;
    }

    private int repairTop()
    {
        return (height - repairPanelHeight(menu.getRepairParts())) / 2;
    }

    private int repairPanelHeight(List<DriveablePart> parts)
    {
        int y = 23;
        int end = visibleRepairEnd(parts);
        for (int index = repairOffset; index < end; index++)
            y += parts.get(index).isDestroyed() ? 40 : 20;
        return y + 8;
    }

    private int visibleRepairEnd(List<DriveablePart> parts)
    {
        int maximumHeight = Math.max(31, height - 20);
        int used = 23;
        int index = Mth.clamp(repairOffset, 0, parts.size());
        while (index < parts.size())
        {
            int rowHeight = parts.get(index).isDestroyed() ? 40 : 20;
            if (used + rowHeight + 8 > maximumHeight && index > repairOffset)
                break;
            used += rowHeight;
            ++index;
        }
        return index;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderLegacyTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = legacyLeft();
        int y = topPos;
        switch (menu.getPage())
        {
            case MENU -> graphics.blit(RenderPipelines.GUI_TEXTURED, MENU_TEXTURE, x, y, 0, 0, LEGACY_WIDTH, LEGACY_HEIGHT, 256, 256);
            case FUEL -> renderFuel(graphics, x, y);
            case REPAIR -> renderRepair(graphics);
            default -> renderInventoryPage(graphics, x, y);
        }
    }

    private void renderInventoryPage(GuiGraphicsExtractor graphics, int x, int y)
    {
        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, x, y, 0, 0, LEGACY_WIDTH, LEGACY_HEIGHT, 256, 256);
        int count = activeItemCount();
        if (menu.getPage() == Page.GUNS)
        {
            int visible = Math.max(0, count - menu.getScrollRow());
            for (int row = 0; row < Math.min(VISIBLE_INVENTORY_ROWS, visible); row++)
                graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, x + 9, y + 24 + row * 19, 176, 0, 37, 18, 256, 256);
            renderGunRows(graphics, x, y);
        }
        else
        {
            int rows = Math.min(VISIBLE_INVENTORY_ROWS, (count + 7) / 8);
            for (int row = 0; row < rows; row++)
            {
                int remaining = count - (menu.getScrollRow() + row) * 8;
                int columns = Math.min(8, Math.max(0, remaining));
                if (columns > 0)
                    graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, x + 9, y + 24 + row * 19, 7, 97, columns * 18, 18, 256, 256);
            }
        }
        if (menu.getScrollRow() == 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, x + 161, y + 41, 176, 18, 10, 10, 256, 256);
        if (menu.getScrollRow() == menu.getMaxScrollRow())
            graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, x + 161, y + 53, 176, 28, 10, 10, 256, 256);
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

    private List<GunRow> gunRows()
    {
        if (menu.getDriveable() == null || menu.getDriveable().getConfigType() == null)
            return List.of();
        DriveableType type = menu.getDriveable().getConfigType();
        List<GunRow> rows = new ArrayList<>();
        for (int index = 0; index < type.getPilotGuns().size(); index++)
        {
            GunType gun = type.getPilotGuns().get(index).getType();
            if (gun != null)
                rows.add(new GunRow("Driver's gun " + (index + 1), gun));
        }
        type.getSeats().stream()
            .filter(seat -> seat != null && seat.getGunType() != null && seat.getGunnerID() >= 0)
            .sorted(Comparator.comparingInt(SeatInfo::getGunnerID))
            .forEach(seat -> {
                String name = seat.getGunName().isBlank() ? "Passenger gun " + (seat.getId() + 1) : seat.getGunName();
                rows.add(new GunRow(name, seat.getGunType()));
            });
        return rows;
    }

    private void renderGunRows(GuiGraphicsExtractor graphics, int x, int y)
    {
        List<GunRow> rows = gunRows();
        for (int visible = 0; visible < VISIBLE_INVENTORY_ROWS; visible++)
        {
            int index = menu.getScrollRow() + visible;
            if (index >= rows.size())
                break;
            GunRow row = rows.get(index);
            int itemY = y + 25 + visible * 19;
            ItemStack gunStack = ModUtils.getItemStack(row.type()).orElse(ItemStack.EMPTY);
            if (!gunStack.isEmpty())
                graphics.item(gunStack, x + 10, itemY);
            graphics.text(font, Component.literal(font.plainSubstrByWidth(row.name(), 55)),
                x + 53, y + 29 + visible * 19, 0x000000, false);

            List<ShootableType> ammo = row.type().getAmmoTypes();
            for (int ammoIndex = 0; ammoIndex < Math.min(3, ammo.size()); ammoIndex++)
            {
                ItemStack ammoStack = ModUtils.getItemStack(ammo.get(ammoIndex)).orElse(ItemStack.EMPTY);
                if (!ammoStack.isEmpty())
                    graphics.item(ammoStack, x + 110 + ammoIndex * 16, itemY);
            }
        }
    }

    private List<BulletType> acceptedVehicleAmmo(EnumSet<EnumWeaponType> weaponTypes)
    {
        if (menu.getDriveable() == null || menu.getDriveable().getConfigType() == null)
            return List.of();
        DriveableType type = menu.getDriveable().getConfigType();
        List<BulletType> candidates = type.isAcceptAllAmmo()
            ? InfoType.getInfoTypes().values().stream().filter(BulletType.class::isInstance).map(BulletType.class::cast).toList()
            : type.getAmmoTypes();
        return candidates.stream()
            .filter(type::isValidAmmo)
            .filter(ammo -> weaponTypes.contains(ammo.getWeaponType()))
            .distinct()
            .sorted(Comparator.comparing(InfoType::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private void renderLegacyTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        if (hoveredSlot != null && hoveredSlot.hasItem())
            return;
        int x = legacyLeft();
        int y = topPos;
        List<Component> lines = new ArrayList<>();
        if (menu.getPage() == Page.MISSILES && mouseX >= x + 10 && mouseX < x + 166
            && mouseY >= y + 20 && mouseY < y + 90)
        {
            lines.add(Component.literal("[" + missilePageName() + "]"));
            acceptedVehicleAmmo(EnumSet.of(EnumWeaponType.MISSILE, EnumWeaponType.SHELL)).stream()
                .map(ammo -> Component.literal("> " + ammo.getName())).forEach(lines::add);
        }
        else if (menu.getPage() == Page.BOMBS && mouseX >= x + 10 && mouseX < x + 166
            && mouseY >= y + 20 && mouseY < y + 90)
        {
            lines.add(Component.literal("[Bombs / Mines]"));
            acceptedVehicleAmmo(EnumSet.of(EnumWeaponType.BOMB, EnumWeaponType.MINE)).stream()
                .map(ammo -> Component.literal("> " + ammo.getName())).forEach(lines::add);
        }
        else if (menu.getPage() == Page.GUNS)
        {
            int row = (mouseY - (y + 25)) / 19;
            List<GunRow> rows = gunRows();
            int index = menu.getScrollRow() + row;
            if (row >= 0 && row < VISIBLE_INVENTORY_ROWS && index < rows.size())
            {
                GunRow gun = rows.get(index);
                if (mouseX >= x + 10 && mouseX < x + 27)
                    lines.add(Component.literal(gun.type().getName()));
                else if (mouseX >= x + 28 && mouseX < x + 46)
                {
                    lines.add(Component.literal("[Ammo]"));
                    gun.type().getAmmoTypes().stream().map(ammo -> Component.literal("> " + ammo.getName())).forEach(lines::add);
                }
            }
        }
        if (!lines.isEmpty())
            graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderFuel(GuiGraphicsExtractor graphics, int x, int y)
    {
        y += 19;
        graphics.blit(RenderPipelines.GUI_TEXTURED, FUEL_TEXTURE, x, y, 0, 0, LEGACY_WIDTH, 161, 256, 256);
        if (menu.getDriveable() == null || menu.getDriveable().getConfigType() == null)
            return;
        float capacity = menu.getDriveable().getConfigType().getFuelTankSize();
        float fraction = capacity <= 0F ? 0F : Mth.clamp(menu.getDriveable().getFuel() / capacity, 0F, 1F);
        int frame = menu.getDriveable().level() == null ? 0 : (int) (menu.getDriveable().level().getGameTime() / 5L % 4L);
        ItemStack fuelStack = menu.getDriveable().getDriveableData().getFuelStack();
        if (!fuelStack.isEmpty())
            graphics.blit(RenderPipelines.GUI_TEXTURED, FUEL_TEXTURE, x + 15, y + 44, 176 + 15 * frame, 0, 15, 16, 256, 256);
        if (capacity > 0F && menu.getDriveable().getFuel() < capacity / 8F && frame > 1)
            graphics.blit(RenderPipelines.GUI_TEXTURED, FUEL_TEXTURE, x + 16, y + 25, 176, 16, 6, 6, 256, 256);
        int width = Math.round(129F * fraction);
        if (width > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, FUEL_TEXTURE, x + 26, y + 21, 0, 161, width, 15, 256, 256);
    }

    private void renderRepair(GuiGraphicsExtractor graphics)
    {
        List<DriveablePart> parts = menu.getRepairParts();
        int left = repairLeft();
        int top = repairTop();
        int end = visibleRepairEnd(parts);
        graphics.blit(RenderPipelines.GUI_TEXTURED, REPAIR_TEXTURE, left, top, 0, 0, 202, 23, 256, 256);
        String vehicleName = menu.getDriveable() == null || menu.getDriveable().getConfigType() == null
            ? title.getString() : menu.getDriveable().getConfigType().getName();
        graphics.text(font, vehicleName + " - Repair", left + 7, top + 7, 0xFFFFFF, false);
        int y = 23;
        for (int index = repairOffset; index < end; index++)
        {
            DriveablePart part = parts.get(index);
            boolean broken = part.isDestroyed();
            int height = broken ? 40 : 20;
            graphics.blit(RenderPipelines.GUI_TEXTURED, REPAIR_TEXTURE, left, top + y, 0, 24, 202, height, 256, 256);

            float health = part.getMaxHealth() <= 0F ? 0F : Mth.clamp(part.getHealth() / part.getMaxHealth(), 0F, 1F);
            int healthColor = 0xFF000000 | (Math.round((1F - health) * 255F) << 16) | (Math.round(health * 255F) << 8);
            graphics.blit(RenderPipelines.GUI_TEXTURED, REPAIR_TEXTURE, left + 111, top + y + 2, 0, 73,
                Math.round(70F * health), 16, 256, 256, healthColor);

            int nameX = broken ? 60 : 10;
            graphics.text(font, Component.literal(font.plainSubstrByWidth(part.getType().getName(), broken ? 48 : 95)),
                left + nameX, top + y + 6, 0xFFFFFF, false);
            graphics.centeredText(font, Math.round(health * 100F) + "%", left + 148, top + y + 6, 0xFFFFFF);

            if (broken && menu.getDriveable() != null)
            {
                List<ItemStack> required = menu.getDriveable().getConfigType().getItemsRequired(part,
                    menu.getDriveable().getDriveableData().getEngine());
                for (int item = 0; item < Math.min(7, required.size()); item++)
                {
                    int itemX = left + 57 + item * 18;
                    int itemY = top + y + 22;
                    graphics.item(required.get(item), itemX, itemY);
                    graphics.itemDecorations(font, required.get(item), itemX, itemY);
                }
            }
            y += height;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, REPAIR_TEXTURE, left, top + y, 0, 65, 202, 8, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        String vehicleName = menu.getDriveable() == null || menu.getDriveable().getConfigType() == null
            ? title.getString() : menu.getDriveable().getConfigType().getName();
        if (menu.getPage() == Page.REPAIR)
            return;

        String pageName = menu.getPage() == Page.MISSILES ? missilePageName() : menu.getPage().getDisplayName();
        String suffix = menu.getPage() == Page.MENU ? "" : " - " + pageName;
        int titleY = menu.getPage() == Page.FUEL ? 25 : 6;
        graphics.text(font, Component.literal(font.plainSubstrByWidth(vehicleName + suffix, 155)),
            LEGACY_X_OFFSET + 6, titleY, 0x404040, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
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
        if (menu.getPage() == Page.REPAIR && mouseX > repairLeft() + 185 && mouseX < repairLeft() + 195
            && mouseY > repairTop() + 5 && mouseY < repairTop() + 15)
        {
            selectPage(Page.MENU);
            return true;
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled && menu.getPage() != Page.MENU)
            clearButtonFocus();
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (menu.getPage() == Page.REPAIR)
        {
            List<DriveablePart> parts = menu.getRepairParts();
            if (scrollY < 0D && visibleRepairEnd(parts) < parts.size())
                ++repairOffset;
            else if (scrollY > 0D && repairOffset > 0)
                --repairOffset;
            refreshButtons();
            return true;
        }
        if (menu.getPage() != Page.MENU && menu.getPage() != Page.FUEL)
        {
            int id = scrollY < 0D ? DriveableInventoryMenu.SCROLL_DOWN_BUTTON : DriveableInventoryMenu.SCROLL_UP_BUTTON;
            sendMenuButton(id);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
