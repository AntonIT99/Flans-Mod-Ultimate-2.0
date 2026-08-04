package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.PartItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.util.InventoryHelper;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
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

    private static final int COLUMN_COUNT = 8;
    private static final int VISIBLE_ROWS = 3;
    private static final int VISIBLE_SLOTS = COLUMN_COUNT * VISIBLE_ROWS;
    private static final int SLOT_SIZE = 18;

    public enum Page
    {
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
    private final DriveableMappedSlot[] mappedSlots = new DriveableMappedSlot[VISIBLE_SLOTS];
    @Getter private Page page = Page.GUNS;
    @Getter private int scrollRow;
    private final int playerInventoryStart;
    private final int playerInventoryEnd;

    public static DriveableInventoryMenu createFromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buffer)
    {
        Entity entity = inventory.player.level().getEntity(buffer.readVarInt());
        return new DriveableInventoryMenu(containerId, inventory, entity instanceof Driveable found ? found : null);
    }

    public DriveableInventoryMenu(int containerId, Inventory playerInventory, @Nullable Driveable driveable)
    {
        super(FlansMod.driveableInventoryMenu.get(), containerId);
        this.playerInventory = playerInventory;
        this.driveable = driveable;
        driveableInventory = driveable == null || driveable.getDriveableData() == null
            ? new SimpleContainer(1) : driveable.getDriveableData();

        for (int row = 0; row < VISIBLE_ROWS; row++)
        {
            for (int column = 0; column < COLUMN_COUNT; column++)
            {
                int visibleIndex = row * COLUMN_COUNT + column;
                DriveableMappedSlot slot = new DriveableMappedSlot(driveableInventory,
                    8 + column * SLOT_SIZE, 31 + row * SLOT_SIZE, this::mayPlaceOnCurrentPage);
                mappedSlots[visibleIndex] = slot;
                addSlot(slot);
            }
        }

        playerInventoryStart = slots.size();
        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * SLOT_SIZE, 103 + row * SLOT_SIZE));
        }
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, 8 + column * SLOT_SIZE, 161));
        playerInventoryEnd = slots.size();
        if (!hasPage(page))
            page = Arrays.stream(Page.values()).filter(this::hasPage).findFirst().orElse(Page.CARGO);
        remapVisibleSlots();
    }

    public int getEntityId()
    {
        return driveable == null ? -1 : driveable.getId();
    }

    public boolean hasPage(Page candidate)
    {
        if (!(driveableInventory instanceof DriveableData data) || driveable == null || driveable.getConfigType() == null)
            return false;
        return switch (candidate)
        {
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
        int rows = (getPageItemCount() + COLUMN_COUNT - 1) / COLUMN_COUNT;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    public List<DriveablePart> getRepairParts()
    {
        if (!(driveableInventory instanceof DriveableData data))
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
            case GUNS -> data.getNumAmmoSlots();
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
            case GUNS -> data.getAmmoInventoryStart();
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
        int start = getPageStart();
        int count = getPageItemCount();
        int offset = scrollRow * COLUMN_COUNT;
        for (int visible = 0; visible < mappedSlots.length; visible++)
        {
            int relative = offset + visible;
            mappedSlots[visible].mapTo(start >= 0 && relative < count ? start + relative : -1);
        }
    }

    private boolean mayPlaceOnCurrentPage(ItemStack stack)
    {
        if (stack.isEmpty() || driveable == null || driveable.getConfigType() == null)
            return false;
        return switch (page)
        {
            case GUNS -> isAmmo(stack, EnumWeaponType.GUN);
            case BOMBS -> isAmmo(stack, EnumWeaponType.BOMB) || isAmmo(stack, EnumWeaponType.MINE);
            case MISSILES -> isAmmo(stack, EnumWeaponType.MISSILE) || isAmmo(stack, EnumWeaponType.SHELL);
            case CARGO -> true;
            case FUEL -> stack.getItem() instanceof PartItem part
                && part.getConfigType().getCategory() == PartType.Category.FUEL;
            // The mapped DriveableData slot enforces the exact mecha slot type,
            // including legacy gun tools and bullet-fed arm slots.
            case ADDONS -> true;
            case REPAIR -> false;
        };
    }

    private boolean isAmmo(ItemStack stack, EnumWeaponType weaponType)
    {
        if (!(stack.getItem() instanceof BulletItem bulletItem))
            return false;
        BulletType bullet = bulletItem.getConfigType();
        return driveable != null && driveable.getConfigType().isValidAmmo(bullet)
            && (bullet.getWeaponType() == weaponType || weaponType == EnumWeaponType.GUN && bullet.getWeaponType() == EnumWeaponType.NONE);
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return driveable != null && driveable.isAlive() && driveable.canPlayerAccessInventory(player);
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

        if (index < VISIBLE_SLOTS)
        {
            if (!moveItemStackTo(sourceStack, playerInventoryStart, playerInventoryEnd, true))
                return ItemStack.EMPTY;
        }
        else if (!moveItemStackTo(sourceStack, 0, VISIBLE_SLOTS, false))
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
}
