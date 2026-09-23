package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.item.PartItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.util.InventoryHelper;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** One compact, scrollable menu for every driveable inventory and repair page. */
public final class DriveableInventoryMenu extends AbstractContainerMenu
{
    public static final int PAGE_BUTTON_BASE = 0;
    public static final int SCROLL_UP_BUTTON = 20;
    public static final int SCROLL_DOWN_BUTTON = 21;
    public static final int REPAIR_BUTTON_BASE = 100;

    private static final int GUI_X_OFFSET = 13;
    private static final int COLUMN_COUNT = 8;
    private static final int VISIBLE_ROWS = 3;
    private static final int GRID_SLOT_COUNT = COLUMN_COUNT * VISIBLE_ROWS;
    private static final int GUN_SLOT_COUNT = 3;
    private static final int SLOT_SIZE = 18;

    public enum Page
    {
        MENU("Menu"),
        GUNS("Guns"),
        BOMBS("Bombs"),
        MISSILES("Missiles"),
        CARGO("Cargo"),
        FUEL("Fuel"),
        ADDONS("Addons"),
        REPAIR("Repair");

        @Getter private final String displayName;

        Page(String displayName)
        {
            this.displayName = displayName;
        }
    }

    @Nullable @Getter private final Driveable driveable;
    private final Inventory playerInventory;
    private final Container driveableInventory;
    private final DriveableMappedSlot[] gridSlots = new DriveableMappedSlot[GRID_SLOT_COUNT];
    private final DriveableMappedSlot[] gunSlots = new DriveableMappedSlot[GUN_SLOT_COUNT];
    private final DriveableMappedSlot fuelSlot;
    @Getter private Page page = Page.MENU;
    @Getter private int scrollRow;
    /** Seat whose single passenger-gun slot this menu exposes, or -1 for the ordinary driver menu. */
    @Getter private final int passengerSeatIndex;
    private final int driveableSlotEnd;
    private final int playerInventoryStart;
    private final int playerInventoryEnd;

    public static DriveableInventoryMenu createFromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer)
    {
        Entity entity = inventory.player.level().getEntity(buffer.readVarInt());
        Page[] pages = Page.values();
        int pageIndex = buffer.readVarInt();
        int passengerSeatIndex = buffer.readVarInt();
        return new DriveableInventoryMenu(containerId, inventory, entity instanceof Driveable found ? found : null,
            pageIndex >= 0 && pageIndex < pages.length ? pages[pageIndex] : Page.MENU, passengerSeatIndex);
    }

    public DriveableInventoryMenu(int containerId, Inventory playerInventory, @Nullable Driveable driveable)
    {
        this(containerId, playerInventory, driveable, Page.MENU);
    }

    public DriveableInventoryMenu(int containerId, Inventory playerInventory, @Nullable Driveable driveable, Page initialPage)
    {
        this(containerId, playerInventory, driveable, initialPage, -1);
    }

    public DriveableInventoryMenu(int containerId, Inventory playerInventory, @Nullable Driveable driveable,
                                  Page initialPage, int passengerSeatIndex)
    {
        super(FlansMod.driveableInventoryMenu.get(), containerId);
        this.playerInventory = playerInventory;
        this.driveable = driveable;
        this.passengerSeatIndex = passengerSeatIndex;
        driveableInventory = driveable == null || driveable.getDriveableData() == null
            ? new SimpleContainer(1) : driveable.getDriveableData();

        for (int row = 0; row < GUN_SLOT_COUNT; row++)
        {
            DriveableMappedSlot slot = new DriveableMappedSlot(driveableInventory,
                GUI_X_OFFSET + 29, 25 + row * 19, this::mayPlaceOnCurrentPage);
            gunSlots[row] = slot;
            addSlot(slot);
        }
        for (int row = 0; row < VISIBLE_ROWS; row++)
        {
            for (int column = 0; column < COLUMN_COUNT; column++)
            {
                int visibleIndex = row * COLUMN_COUNT + column;
                DriveableMappedSlot slot = new DriveableMappedSlot(driveableInventory,
                    GUI_X_OFFSET + 10 + column * SLOT_SIZE, 25 + row * 19, this::mayPlaceOnCurrentPage);
                gridSlots[visibleIndex] = slot;
                addSlot(slot);
            }
        }
        fuelSlot = new DriveableMappedSlot(driveableInventory, GUI_X_OFFSET + 35, 63, this::mayPlaceOnCurrentPage);
        addSlot(fuelSlot);
        driveableSlotEnd = slots.size();

        playerInventoryStart = slots.size();
        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
                addSlot(new PageAwarePlayerSlot(playerInventory, column + row * 9 + 9,
                    GUI_X_OFFSET + 8 + column * SLOT_SIZE, 98 + row * SLOT_SIZE));
        }
        for (int column = 0; column < 9; column++)
            addSlot(new PageAwarePlayerSlot(playerInventory, column, GUI_X_OFFSET + 8 + column * SLOT_SIZE, 156));
        playerInventoryEnd = slots.size();
        if (isPassengerGunMenu())
            page = Page.GUNS;
        else if (hasPage(initialPage))
            page = initialPage;
        remapVisibleSlots();
    }

    public boolean isPassengerGunMenu()
    {
        return passengerSeatIndex >= 0;
    }

    /** First ammo index visible on the Guns page, relative to the driveable ammo range. */
    public int getVisibleGunStart()
    {
        return isPassengerGunMenu() ? getPassengerGunAmmoIndex() : 0;
    }

    public int getVisibleGunCount()
    {
        if (!(driveableInventory instanceof DriveableData data))
            return 0;
        return isPassengerGunMenu() ? (getPassengerGunAmmoIndex() >= 0 ? 1 : 0) : data.getNumAmmoSlots();
    }

    private int getPassengerGunAmmoIndex()
    {
        if (driveable == null || driveable.getConfigType() == null)
            return -1;
        SeatInfo info = driveable.getConfigType().getSeat(passengerSeatIndex);
        int ammoIndex = info == null || info.isDriver() || info.getGunType() == null ? -1 : info.getGunnerID();
        return ammoIndex >= 0 && ammoIndex < driveable.getConfigType().getNumAmmoSlots() ? ammoIndex : -1;
    }

    public int getEntityId()
    {
        return driveable == null ? -1 : driveable.getId();
    }

    public boolean hasPage(Page candidate)
    {
        if (!(driveableInventory instanceof DriveableData data) || driveable == null || driveable.getConfigType() == null)
            return false;
        if (isPassengerGunMenu())
            return candidate == Page.GUNS && getPassengerGunAmmoIndex() >= 0;
        return switch (candidate)
        {
            case MENU -> true;
            case GUNS -> data.getNumAmmoSlots() > 0;
            case BOMBS -> data.getNumBombSlots() > 0;
            case MISSILES -> data.getNumMissileSlots() > 0;
            case CARGO -> data.getNumCargoSlots() > 0;
            case FUEL -> driveable.getConfigType().getFuelTankSize() > 0F;
            case ADDONS -> data.getNumMechaSlots() > 0;
            case REPAIR -> getRepairParts().stream().anyMatch(part -> part.getMaxHealth() > 0F);
        };
    }

    public int getMaxScrollRow()
    {
        if (page == Page.GUNS)
            return Math.max(0, getPageItemCount() - GUN_SLOT_COUNT);
        int rows = (getPageItemCount() + COLUMN_COUNT - 1) / COLUMN_COUNT;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    public List<DriveablePart> getRepairParts()
    {
        if (isPassengerGunMenu() || !(driveableInventory instanceof DriveableData data))
            return List.of();
        return data.getParts().values().stream()
            .filter(part -> part.getMaxHealth() > 0F)
            .sorted(Comparator.comparing(part -> part.getType().getName(), String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id)
    {
        if (!stillValid(player))
            return false;
        if (id >= PAGE_BUTTON_BASE && id < PAGE_BUTTON_BASE + Page.values().length)
        {
            Page requested = Page.values()[id - PAGE_BUTTON_BASE];
            if (!hasPage(requested))
                return false;
            page = requested;
            scrollRow = 0;
            remapVisibleSlots();
            return true;
        }
        if (id == SCROLL_UP_BUTTON && scrollRow > 0)
        {
            --scrollRow;
            remapVisibleSlots();
            return true;
        }
        if (id == SCROLL_DOWN_BUTTON && scrollRow < getMaxScrollRow())
        {
            ++scrollRow;
            remapVisibleSlots();
            return true;
        }
        if (id >= REPAIR_BUTTON_BASE && id < REPAIR_BUTTON_BASE + EnumDriveablePart.values().length)
        {
            if (!player.level().isClientSide)
                repairPart(player, EnumDriveablePart.values()[id - REPAIR_BUTTON_BASE]);
            return true;
        }
        return false;
    }

    private void repairPart(Player player, EnumDriveablePart partType)
    {
        if (driveable == null || page != Page.REPAIR || !(driveableInventory instanceof DriveableData data))
            return;
        DriveablePart part = data.getPart(partType);
        if (part == null || !part.isDestroyed() || part.getMaxHealth() <= 0F)
            return;
        if (Arrays.stream(partType.getParents()).anyMatch(parent -> !driveable.isPartIntact(parent)))
            return;

        List<ItemStack> required = driveable.getConfigType().getItemsRequired(part, data.getEngine());
        boolean creative = player.getAbilities().instabuild;
        if (!creative && !InventoryHelper.canConsumeAll(player.getInventory(), required))
            return;
        if (!driveable.repairPart(partType, Math.max(1F, part.getMaxHealth() / 10F)))
            return;
        InventoryHelper.tryConsumeAll(player.getInventory(), required, creative);
    }

    private int getPageItemCount()
    {
        if (!(driveableInventory instanceof DriveableData data))
            return 0;
        return switch (page)
        {
            case MENU -> 0;
            case GUNS -> getVisibleGunCount();
            case BOMBS -> data.getNumBombSlots();
            case MISSILES -> data.getNumMissileSlots();
            case CARGO -> data.getNumCargoSlots();
            case FUEL -> 1;
            case ADDONS -> data.getNumMechaSlots();
            case REPAIR -> 0;
        };
    }

    private int getPageStart()
    {
        if (!(driveableInventory instanceof DriveableData data))
            return -1;
        return switch (page)
        {
            case MENU -> -1;
            // Driver and passenger menus deliberately map onto the same DriveableData slot.
            // Server-side menu clicks are serialized, and each open menu broadcasts that shared state.
            case GUNS -> data.getAmmoInventoryStart() + getVisibleGunStart();
            case BOMBS -> data.getBombInventoryStart();
            case MISSILES -> data.getMissileInventoryStart();
            case CARGO -> data.getCargoInventoryStart();
            case FUEL -> data.getFuelSlot();
            case ADDONS -> data.getMechaInventoryStart();
            case REPAIR -> -1;
        };
    }

    private void remapVisibleSlots()
    {
        for (DriveableMappedSlot slot : gunSlots)
            slot.mapTo(-1);
        for (DriveableMappedSlot slot : gridSlots)
            slot.mapTo(-1);
        fuelSlot.mapTo(-1);

        int start = getPageStart();
        int count = getPageItemCount();
        if (start < 0 || count <= 0)
            return;
        if (page == Page.GUNS)
        {
            for (int visible = 0; visible < gunSlots.length; visible++)
            {
                int relative = scrollRow + visible;
                gunSlots[visible].mapTo(relative < count ? start + relative : -1);
            }
            return;
        }
        if (page == Page.FUEL)
        {
            fuelSlot.mapTo(start);
            return;
        }
        int offset = scrollRow * COLUMN_COUNT;
        for (int visible = 0; visible < gridSlots.length; visible++)
        {
            int relative = offset + visible;
            gridSlots[visible].mapTo(relative < count ? start + relative : -1);
        }
    }

    private boolean mayPlaceOnCurrentPage(ItemStack stack)
    {
        if (stack.isEmpty() || driveable == null || driveable.getConfigType() == null)
            return false;
        return switch (page)
        {
            case MENU -> false;
            // Legacy FilterAmmunitionInput accepts bullets and grenades in all
            // weapon pages; when disabled it intentionally accepts any item.
            case GUNS, BOMBS, MISSILES -> !driveable.getConfigType().isFilterAmmunition()
                || stack.getItem() instanceof ShootableItem;
            case CARGO -> true;
            case FUEL -> stack.getItem() instanceof PartItem part
                && part.getConfigType().getCategory() == PartType.Category.FUEL;
            // The mapped DriveableData slot enforces the exact mecha slot type,
            // including legacy gun tools and bullet-fed arm slots.
            case ADDONS -> true;
            case REPAIR -> false;
        };
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        if (driveable == null || !driveable.isAlive() || !driveable.canPlayerAccessInventory(player))
            return false;
        if (!isPassengerGunMenu())
            return true;
        Seat seat = driveable.getSeat(player);
        return seat != null && !seat.isDriverSeat() && seat.getSeatIndex() == passengerSeatIndex
            && getPassengerGunAmmoIndex() >= 0;
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index)
    {
        if (index < 0 || index >= slots.size())
            return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem())
            return ItemStack.EMPTY;
        ItemStack sourceStack = source.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < driveableSlotEnd)
        {
            if (!moveItemStackTo(sourceStack, playerInventoryStart, playerInventoryEnd, true))
                return ItemStack.EMPTY;
        }
        else if (!moveItemStackTo(sourceStack, 0, driveableSlotEnd, false))
        {
            int hotbarStart = playerInventoryEnd - 9;
            if (index < hotbarStart)
            {
                if (!moveItemStackTo(sourceStack, hotbarStart, playerInventoryEnd, false))
                    return ItemStack.EMPTY;
            }
            else if (!moveItemStackTo(sourceStack, playerInventoryStart, hotbarStart, false))
                return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty())
            source.set(ItemStack.EMPTY);
        else
            source.setChanged();
        if (sourceStack.getCount() == copy.getCount())
            return ItemStack.EMPTY;
        source.onTake(player, sourceStack);
        return copy;
    }

    private final class PageAwarePlayerSlot extends Slot
    {
        private PageAwarePlayerSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive()
        {
            return page != Page.REPAIR;
        }
    }
}
