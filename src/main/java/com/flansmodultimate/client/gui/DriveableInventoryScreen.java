package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.PilotGun;
import com.flansmodultimate.common.driveables.armor.ArmorPlate;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu.Page;
import com.flansmodultimate.common.item.AmmoStatContext;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.item.TooltipKeys;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.util.InventoryHelper;
import com.flansmodultimate.util.ModUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** 1.7.10-style driveable hub, inventory, fuel and repair interface. */
public final class DriveableInventoryScreen extends AbstractContainerScreen<DriveableInventoryMenu>
{
    private static final int LEGACY_WIDTH = 176;
    private static final int LEGACY_HEIGHT = 180;
    private static final int LEGACY_X_OFFSET = 13;
    private static final int VISIBLE_INVENTORY_ROWS = 3;
    private final Map<Page, Button> pageButtons = new EnumMap<>(Page.class);
    private final List<Button> repairButtons = new ArrayList<>();
    private int repairOffset;
    private String ammoTooltipKey = "";
    private int ammoTooltipPage;
    private int ammoTooltipPageCount;
    private boolean ammoTooltipPagesVisible;

    /** @param pilotGun the bank-mounted driver gun, or null for a passenger seat's gun */
    private record GunRow(String name, GunType type, @Nullable PilotGun pilotGun) {}

    public DriveableInventoryScreen(DriveableInventoryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = 202;
        imageHeight = LEGACY_HEIGHT;
        inventoryLabelX = LEGACY_X_OFFSET + 8;
        inventoryLabelY = 86;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderLegacyTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        int x = legacyLeft();
        int y = topPos;
        switch (menu.getPage())
        {
            case MENU -> graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEMENU, x, y, 0, 0, LEGACY_WIDTH, LEGACY_HEIGHT);
            case FUEL -> renderFuel(graphics, x, y);
            case REPAIR -> renderRepair(graphics);
            default -> renderInventoryPage(graphics, x, y);
        }
    }

    private void renderInventoryPage(GuiGraphics graphics, int x, int y)
    {
        graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEINVENTORY, x, y, 0, 0, LEGACY_WIDTH, LEGACY_HEIGHT);
        int count = activeItemCount();
        if (menu.getPage() == Page.GUNS)
        {
            int visible = Math.max(0, count - menu.getScrollRow());
            for (int row = 0; row < Math.min(VISIBLE_INVENTORY_ROWS, visible); row++)
                graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEINVENTORY, x + 9, y + 24 + row * 19, 176, 0, 37, 18);
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
                    graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEINVENTORY, x + 9, y + 24 + row * 19, 7, 97, columns * 18, 18);
            }
        }
        if (menu.getScrollRow() == 0)
            graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEINVENTORY, x + 161, y + 41, 176, 18, 10, 10);
        if (menu.getScrollRow() == menu.getMaxScrollRow())
            graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEINVENTORY, x + 161, y + 53, 176, 28, 10, 10);
    }

    private int activeItemCount()
    {
        if (menu.getDriveable() == null || menu.getDriveable().getDriveableData() == null)
            return 0;
        var data = menu.getDriveable().getDriveableData();
        return switch (menu.getPage())
        {
            case GUNS -> menu.getVisibleGunCount();
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
        int first = menu.getVisibleGunStart();
        int end = first + menu.getVisibleGunCount();
        for (int ammoIndex = first; ammoIndex < end; ammoIndex++)
        {
            if (ammoIndex < type.getNumPassengerGunners())
            {
                int currentAmmoIndex = ammoIndex;
                type.getSeats().stream()
                    .filter(seat -> seat != null && seat.getGunnerID() == currentAmmoIndex && seat.getGunType() != null)
                    .findFirst()
                    .ifPresent(seat -> {
                        String name = seat.getGunName().isBlank()
                            ? "Passenger gun " + (seat.getId() + 1) : seat.getGunName();
                        rows.add(new GunRow(name, seat.getGunType(), null));
                    });
                continue;
            }
            int pilotIndex = ammoIndex - type.getNumPassengerGunners();
            if (pilotIndex >= 0 && pilotIndex < type.getPilotGuns().size())
            {
                PilotGun pilotGun = type.getPilotGuns().get(pilotIndex);
                GunType gun = pilotGun.getType();
                if (gun != null)
                    rows.add(new GunRow("Driver's gun " + (pilotIndex + 1), gun, pilotGun));
            }
        }
        return rows;
    }

    private void renderGunRows(GuiGraphics graphics, int x, int y)
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
                graphics.renderItem(gunStack, x + 10, itemY);
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(row.name(), 55)),
                x + 53, y + 29 + visible * 19, 0x000000, false);

            List<ShootableType> ammo = row.type().getAmmoTypes();
            for (int ammoIndex = 0; ammoIndex < Math.min(3, ammo.size()); ammoIndex++)
            {
                ItemStack ammoStack = ModUtils.getItemStack(ammo.get(ammoIndex)).orElse(ItemStack.EMPTY);
                if (!ammoStack.isEmpty())
                    graphics.renderItem(ammoStack, x + 110 + ammoIndex * 16, itemY);
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

    private void renderLegacyTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        ammoTooltipPagesVisible = false;
        if (hoveredSlot != null && hoveredSlot.hasItem())
            return;
        int x = legacyLeft();
        int y = topPos;
        List<Component> lines = new ArrayList<>();
        if (menu.getPage() == Page.REPAIR)
            lines.addAll(repairTooltip(mouseX, mouseY));
        else if (menu.getPage() == Page.MISSILES && mouseX >= x + 10 && mouseX < x + 166
            && mouseY >= y + 20 && mouseY < y + 90)
        {
            lines.addAll(ammoTooltip("missiles", Component.literal("[" + missilePageName() + "]"),
                acceptedVehicleAmmo(EnumSet.of(EnumWeaponType.MISSILE, EnumWeaponType.SHELL)), bankContext(true)));
        }
        else if (menu.getPage() == Page.BOMBS && mouseX >= x + 10 && mouseX < x + 166
            && mouseY >= y + 20 && mouseY < y + 90)
        {
            lines.addAll(ammoTooltip("bombs", Component.literal("[Bombs / Mines]"),
                acceptedVehicleAmmo(EnumSet.of(EnumWeaponType.BOMB, EnumWeaponType.MINE)), bankContext(false)));
        }
        else if (menu.getPage() == Page.GUNS && mouseY >= y + 25)
        {
            int row = (mouseY - (y + 25)) / 19;
            List<GunRow> rows = gunRows();
            int index = menu.getScrollRow() + row;
            if (row >= 0 && row < VISIBLE_INVENTORY_ROWS && index < rows.size())
            {
                GunRow gun = rows.get(index);
                List<ShootableType> ammo = gun.type().getAmmoTypes();
                int ammoIcon = (mouseX - (x + 110)) / 16;
                if (mouseX >= x + 10 && mouseX < x + 27 || mouseX >= x + 53 && mouseX < x + 108)
                    lines.addAll(gunTooltip(gun));
                else if (mouseX >= x + 28 && mouseX < x + 46)
                    lines.addAll(ammoTooltip("gun:" + index, Component.literal("[Ammo]"), ammo, gunContext(gun)));
                else if (mouseX >= x + 110 && ammoIcon < Math.min(3, ammo.size()) && mouseY < y + 25 + row * 19 + 16)
                    lines.addAll(ammoTooltip("gun:" + index + ":" + ammoIcon, Component.literal(gun.name()),
                        List.of(ammo.get(ammoIcon)), gunContext(gun)));
            }
        }
        if (!lines.isEmpty())
            graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    /**
     * Ammunition list tooltip. Holding Shift expands every entry with the same stats as the
     * ammo item's own detailed tooltip; entries that do not fit on screen are paged with Shift + scroll.
     */
    private List<Component> ammoTooltip(String key, Component header, List<? extends ShootableType> ammo,
                                        Function<ShootableType, AmmoStatContext> contexts)
    {
        if (!key.equals(ammoTooltipKey))
        {
            ammoTooltipKey = key;
            ammoTooltipPage = 0;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(header);
        if (!hasShiftDown())
        {
            ammo.forEach(type -> lines.add(Component.literal("> ").append(ModUtils.getDisplayName(type))));
            if (!ammo.isEmpty() && minecraft != null)
            {
                Component keyName = minecraft.options.keyShift.getTranslatedKeyMessage().copy()
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
                lines.add(Component.translatable(TooltipKeys.HOLD_FOR_DETAILS, keyName).withStyle(ChatFormatting.GRAY));
            }
            return lines;
        }

        // Header and page footer take two lines; tooltip lines are 10 px tall.
        int maxLines = Math.max(4, (height - 24) / 10 - 2);
        List<List<Component>> pages = new ArrayList<>();
        List<Component> page = new ArrayList<>();
        List<List<Component>> blocks = new ArrayList<>();
        for (ShootableType type : ammo)
            blocks.addAll(ammoBlocks(type, contexts.apply(type)));
        for (List<Component> block : blocks)
        {
            if (!page.isEmpty() && page.size() + block.size() > maxLines)
            {
                pages.add(page);
                page = new ArrayList<>();
            }
            page.addAll(block);
        }
        if (!page.isEmpty())
            pages.add(page);

        ammoTooltipPageCount = pages.size();
        ammoTooltipPagesVisible = pages.size() > 1;
        if (pages.isEmpty())
            return lines;
        ammoTooltipPage = Mth.clamp(ammoTooltipPage, 0, pages.size() - 1);
        lines.addAll(pages.get(ammoTooltipPage));
        if (pages.size() > 1)
            lines.add(Component.translatable("gui.flansmodultimate.driveable.ammo_page",
                ammoTooltipPage + 1, pages.size()).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    /**
     * One block per ammunition, or one per round when the weapon feeds a round mix,
     * so each round pages with the mouse wheel as if it were its own ammunition.
     */
    private List<List<Component>> ammoBlocks(ShootableType type, @Nullable AmmoStatContext context)
    {
        if (context == null || !(type instanceof BulletType bullet))
            return List.of(ammoDetails(type, context, null));
        List<BulletType.RoundEntry> rounds = context.rounds(bullet);
        if (rounds.isEmpty())
            return List.of(ammoDetails(type, context, null));

        List<List<Component>> blocks = new ArrayList<>();
        int shot = 0;
        for (BulletType.RoundEntry round : rounds)
        {
            blocks.add(ammoDetails(type, context.withFixedShot(shot), round.name() + " (" + round.count() + ")"));
            shot += round.count();
        }
        return blocks;
    }

    /** Stats of one ammunition type as the vehicle in use fires it; a null context describes the ammo alone. */
    private List<Component> ammoDetails(ShootableType type, @Nullable AmmoStatContext context, @Nullable String roundName)
    {
        List<Component> block = new ArrayList<>();
        block.add(Component.literal("> ").append(ModUtils.getDisplayName(type))
            .append(roundName == null ? "" : " - " + roundName)
            .withStyle(ChatFormatting.YELLOW));

        List<Component> stats = new ArrayList<>();
        if (type.getRoundsPerItem() > 1)
            stats.add(IFlanItem.statLine(Component.translatable(TooltipKeys.ROUNDS), String.valueOf(type.getRoundsPerItem())));
        ModUtils.getItemStack(type)
            .map(ItemStack::getItem)
            .filter(ShootableItem.class::isInstance)
            .map(ShootableItem.class::cast)
            .ifPresent(item -> item.appendAmmoStats(stats, context));
        stats.forEach(line -> block.add(Component.literal("  ").append(line)));
        return block;
    }

    /**
     * A mounted gun's stats as the vehicle in use fires it. Driver guns take their bank's fire rate, fire mode
     * and damage multiplier (or the vehicle's ballistics for a ranging gun); passenger guns fire as the gun alone.
     * Damage is listed per accepted ammunition, with the gun's and the vehicle's ammo overrides applied.
     */
    private List<Component> gunTooltip(GunRow row)
    {
        GunType gun = row.type();
        List<Component> lines = new ArrayList<>();
        lines.add(ModUtils.getDisplayName(gun).copy().withStyle(ChatFormatting.YELLOW));

        if (!hasShiftDown())
        {
            if (minecraft != null)
            {
                Component keyName = minecraft.options.keyShift.getTranslatedKeyMessage().copy()
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
                lines.add(Component.translatable(TooltipKeys.HOLD_FOR_DETAILS, keyName).withStyle(ChatFormatting.GRAY));
            }
            return lines;
        }

        var driveable = menu.getDriveable();
        if (driveable == null || driveable.getConfigType() == null)
            return lines;

        boolean bank = row.pilotGun() != null;
        boolean secondary = bank && driveable.getConfigType().shootPoints(true).stream()
            .anyMatch(point -> point.getRootPos() == row.pilotGun());
        FireableGun fireable = bank ? driveable.getWeaponBankFireableGun(gun, secondary) : new FireableGun(gun);
        float shootDelay = bank ? driveable.getWeaponBankShootDelay(secondary) : gun.getShootDelay(null);
        EnumFireMode mode = bank ? driveable.getWeaponBankFireMode(secondary) : gun.getFireMode(null);

        if (shootDelay > 0F)
            lines.add(IFlanItem.statLine(Component.translatable(TooltipKeys.FIRE_RATE), IFlanItem.formatFloat(1200F / shootDelay) + " rpm"));
        lines.add(IFlanItem.statLine(Component.translatable(TooltipKeys.MODE),
            Component.translatable("tooltip.flansmodultimate.fire_mode." + mode.name().toLowerCase(Locale.ROOT))));
        if (gun.getNumBullets() > 1)
            lines.add(IFlanItem.statLine(Component.translatable(TooltipKeys.SHOT), String.valueOf(gun.getNumBullets())));

        // The weapon's own velocity is only a fallback for ammunition that declares none; see the ammo tooltip.
        float velocity = fireable.getBulletSpeed() * fireable.getBulletSpeedMultiplier();
        lines.add(IFlanItem.statLine(Component.translatable(TooltipKeys.MUZZLE_VELOCITY),
            velocity != 0F ? IFlanItem.formatFloat(velocity * 20F) + " m/s" : "∞"));

        float dispersion = Mth.RAD_TO_DEG * ShootingHelper.ANGULAR_SPREAD_FACTOR * fireable.getSpread();
        if (dispersion > 0F)
            lines.add(IFlanItem.statLine(Component.translatable(TooltipKeys.DISPERSION), IFlanItem.formatFloat(dispersion) + "°"));

        List<ShootableType> ammo = gun.getAmmoTypes();
        if (!ammo.isEmpty())
        {
            Function<ShootableType, AmmoStatContext> contexts = gunContext(row);
            lines.add(Component.translatable(TooltipKeys.DAMAGE).append(":").withStyle(ChatFormatting.BLUE));
            for (ShootableType type : ammo)
                lines.add(IFlanItem.indentedStatLine(ModUtils.getDisplayName(type), mountedAmmoDamage(type, contexts.apply(type))));
        }
        return lines;
    }

    /** Damage of the first round of an ammunition fired through this context, following {@link ShootingHelper#getDamage}. */
    private static String mountedAmmoDamage(ShootableType type, @Nullable AmmoStatContext context)
    {
        if (!(type instanceof BulletType bullet) || context == null)
            return IFlanItem.formatFloat(type.getDamage().getDamage(), 1);
        if (bullet.useKineticDamageSystem())
        {
            FiredShot shot = context.shot(bullet, 0);
            float mass = shot.getProjectileMass();
            if (mass > 0F)
                return IFlanItem.formatFloat(ShootingHelper.getKineticDamage(mass, shot.getMuzzleVelocity()), 1);
        }
        return IFlanItem.formatFloat(bullet.getDamage().getDamage() * context.fireable(bullet).getDamage(), 1);
    }

    /** Resolves a gun row's ammunition the way that gun fires: from its weapon bank, or from a passenger seat. */
    private Function<ShootableType, AmmoStatContext> gunContext(GunRow row)
    {
        var driveable = menu.getDriveable();
        if (driveable == null || driveable.getConfigType() == null)
            return ammo -> null;
        if (row.pilotGun() == null)
            return ammo -> driveable.getPassengerAmmoStatContext(row.type(), ammo);

        boolean secondary = driveable.getConfigType().shootPoints(true).stream()
            .anyMatch(point -> point.getRootPos() == row.pilotGun());
        return ammo -> driveable.getBankAmmoStatContext(row.type(), secondary, ammo);
    }

    /**
     * Resolves shells, missiles, bombs and mines through whichever weapon bank fires their weapon type.
     *
     * @param launchStats false for dropped ordnance, which has no meaningful muzzle velocity or dispersion
     */
    private Function<ShootableType, AmmoStatContext> bankContext(boolean launchStats)
    {
        var driveable = menu.getDriveable();
        if (driveable == null || driveable.getConfigType() == null)
            return ammo -> null;
        DriveableType type = driveable.getConfigType();
        return ammo -> {
            EnumWeaponType weapon = ammo instanceof BulletType bullet ? bullet.getWeaponType() : EnumWeaponType.NONE;
            boolean secondary = type.weaponType(true) == weapon && type.weaponType(false) != weapon;
            return driveable.getBankAmmoStatContext(null, secondary, ammo).withLaunchStats(launchStats);
        };
    }

    /** Tooltip for the repair row whose name or health bar is under the cursor. */
    private List<Component> repairTooltip(int mouseX, int mouseY)
    {
        List<DriveablePart> parts = menu.getRepairParts();
        int left = repairLeft();
        int y = repairTop() + 23;
        int end = visibleRepairEnd(parts);
        for (int index = repairOffset; index < end; index++)
        {
            DriveablePart part = parts.get(index);
            boolean broken = part.isDestroyed();
            // Broken rows start the name after the repair button, so the button keeps its own hover.
            int nameX = left + (broken ? 60 : 10);
            if (mouseY >= y && mouseY < y + 20 && mouseX >= nameX && mouseX < left + 181)
                return partTooltip(part);
            y += broken ? 40 : 20;
        }
        return List.of();
    }

    private List<Component> partTooltip(DriveablePart part)
    {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(part.getType().getName()).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("gui.flansmodultimate.driveable.part_health",
            formatStat(part.getHealth()), formatStat(part.getMaxHealth())).withStyle(ChatFormatting.GRAY));

        DriveableType type = menu.getDriveable() == null ? null : menu.getDriveable().getConfigType();
        if (type != null && type.getResolvedArmor() != null)
        {
            List<Component> plates = new ArrayList<>();
            for (EnumArmorFacing facing : EnumArmorFacing.values())
            {
                ArmorPlate plate = type.getResolvedArmor().plate(part.getType(), facing).authored();
                if (!plate.isArmoured())
                    continue;
                Component facingName = Component.translatable(
                    "gui.flansmodultimate.driveable.facing." + facing.name().toLowerCase(Locale.ROOT));
                Component line = plate.slopeDeg() != 0F
                    ? Component.translatable("gui.flansmodultimate.driveable.armor_plate_sloped",
                        facingName, formatStat(plate.thicknessMm()), formatStat(plate.slopeDeg()),
                        formatStat(lineOfSightArmor(plate)))
                    : Component.translatable("gui.flansmodultimate.driveable.armor_plate",
                        facingName, formatStat(plate.thicknessMm()));
                plates.add(Component.literal("  ").append(line).withStyle(ChatFormatting.GRAY));
            }
            if (!plates.isEmpty())
            {
                lines.add(Component.translatable("gui.flansmodultimate.driveable.armor").withStyle(ChatFormatting.GRAY));
                lines.addAll(plates);
            }
        }

        if (part.getPenetrationResistance() > 0F)
            lines.add(Component.translatable("gui.flansmodultimate.driveable.penetration_resistance",
                formatStat(part.getPenetrationResistance())).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    /**
     * Effective thickness against a level shot straight at the face, resolved the same way as
     * {@code ResolvedVehicleArmor.resolveHit}: nominal thickness over the cosine of the slope,
     * with the slope capped at the configured maximum impact angle.
     */
    private static float lineOfSightArmor(ArmorPlate plate)
    {
        double maxAngle = ModCommonConfig.maxArmorImpactAngleDeg();
        double safeMaxAngle = Double.isFinite(maxAngle) ? Mth.clamp(maxAngle, 0D, 89.9D) : 80D;
        double slope = Mth.clamp(Math.abs(plate.slopeDeg()), 0D, safeMaxAngle);
        return (float) (plate.thicknessMm() / Math.cos(Math.toRadians(slope)));
    }

    private static String formatStat(float value)
    {
        return Math.abs(value - Math.round(value)) < 0.05F
            ? Integer.toString(Math.round(value))
            : String.format(Locale.ROOT, "%.1f", value);
    }

    private void renderFuel(GuiGraphics graphics, int x, int y)
    {
        y += 19;
        graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEFUEL, x, y, 0, 0, LEGACY_WIDTH, 161);
        if (menu.getDriveable() == null || menu.getDriveable().getConfigType() == null)
            return;
        float capacity = menu.getDriveable().getConfigType().getFuelTankSize();
        float fraction = capacity <= 0F ? 0F : Mth.clamp(menu.getDriveable().getFuel() / capacity, 0F, 1F);
        int frame = (int) (menu.getDriveable().level().getGameTime() / 5L % 4L);
        ItemStack fuelStack = Optional.ofNullable(menu.getDriveable().getDriveableData()).map(DriveableData::getFuelStack).orElse(ItemStack.EMPTY);
        if (!fuelStack.isEmpty())
            graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEFUEL, x + 15, y + 44, 176 + 15 * frame, 0, 15, 16);
        if (capacity > 0F && menu.getDriveable().getFuel() < capacity / 8F && frame > 1)
            graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEFUEL, x + 16, y + 25, 176, 16, 6, 6);
        int width = Math.round(129F * fraction);
        if (width > 0)
            graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEFUEL, x + 26, y + 21, 0, 161, width, 15);
    }

    private void renderRepair(GuiGraphics graphics)
    {
        List<DriveablePart> parts = menu.getRepairParts();
        int left = repairLeft();
        int top = repairTop();
        int end = visibleRepairEnd(parts);
        graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEREPAIR, left, top, 0, 0, 202, 23);
        String vehicleName = menu.getDriveable() == null || menu.getDriveable().getConfigType() == null
            ? title.getString() : ModUtils.getDisplayNameString(menu.getDriveable().getConfigType());
        graphics.drawString(font, vehicleName + " - Repair", left + 7, top + 7, 0xFFFFFF, false);
        int y = 23;
        for (int index = repairOffset; index < end; index++)
        {
            DriveablePart part = parts.get(index);
            boolean broken = part.isDestroyed();
            int height = broken ? 40 : 20;
            graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEREPAIR, left, top + y, 0, 24, 202, height);

            float health = part.getMaxHealth() <= 0F ? 0F : Mth.clamp(part.getHealth() / part.getMaxHealth(), 0F, 1F);
            graphics.setColor(1F - health, health, 0F, 1F);
            graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEREPAIR, left + 111, top + y + 2, 0, 73, Math.round(70F * health), 16);
            graphics.setColor(1F, 1F, 1F, 1F);

            int nameX = broken ? 60 : 10;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(part.getType().getName(), broken ? 48 : 95)),
                left + nameX, top + y + 6, 0xFFFFFF, false);
            graphics.drawCenteredString(font, Math.round(health * 100F) + "%", left + 148, top + y + 6, 0xFFFFFF);

            if (broken && menu.getDriveable() != null)
            {
                List<ItemStack> required = menu.getDriveable().getConfigType().getItemsRequired(part,
                    menu.getDriveable().getDriveableData().getEngine());
                for (int item = 0; item < Math.min(7, required.size()); item++)
                {
                    int itemX = left + 57 + item * 18;
                    int itemY = top + y + 22;
                    graphics.renderItem(required.get(item), itemX, itemY);
                    graphics.renderItemDecorations(font, required.get(item), itemX, itemY);
                }
            }
            y += height;
        }
        graphics.blit(FlansMod.TEXTURE_GUI_DRIVEABLEREPAIR, left, top + y, 0, 65, 202, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        String vehicleName = menu.getDriveable() == null || menu.getDriveable().getConfigType() == null
            ? title.getString() : ModUtils.getDisplayNameString(menu.getDriveable().getConfigType());
        if (menu.getPage() == Page.REPAIR)
            return;

        String pageName = menu.getPage() == Page.MISSILES ? missilePageName() : menu.getPage().getDisplayName();
        String suffix = menu.getPage() == Page.MENU ? "" : " - " + pageName;
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
        if (!menu.isPassengerGunMenu() && menu.getPage() != Page.MENU && menu.getPage() != Page.REPAIR
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
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled && menu.getPage() != Page.MENU)
            clearButtonFocus();
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (ammoTooltipPagesVisible && hasShiftDown() && ammoTooltipPageCount > 1)
        {
            int step = delta < 0D ? 1 : -1;
            ammoTooltipPage = Math.floorMod(ammoTooltipPage + step, ammoTooltipPageCount);
            return true;
        }
        if (menu.getPage() == Page.REPAIR)
        {
            List<DriveablePart> parts = menu.getRepairParts();
            if (delta < 0D && visibleRepairEnd(parts) < parts.size())
                ++repairOffset;
            else if (delta > 0D && repairOffset > 0)
                --repairOffset;
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
