package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.EnumMechaSlotType;
import com.flansmodultimate.common.entity.Mecha;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/** The dedicated mecha window: equipment and upgrade slots around the mecha, plus its scrollable cargo. */
public final class MechaInventoryMenu extends AbstractContainerMenu
{
    public static final int SCROLL_UP_BUTTON = 0;
    public static final int SCROLL_DOWN_BUTTON = 1;
    public static final int DRIVEABLE_MENU_BUTTON = 2;

    public static final int COLUMN_COUNT = 8;
    public static final int VISIBLE_ROWS = 3;
    private static final int GRID_SLOT_COUNT = COLUMN_COUNT * VISIBLE_ROWS;
    private static final int CARGO_LEFT = 186;
    private static final int CARGO_TOP = 25;
    private static final int CARGO_ROW_HEIGHT = 19;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INVENTORY_LEFT = 182;

    /** Legacy 1.12.2 positions of every mecha equipment and upgrade slot. */
    private static final Map<EnumMechaSlotType, int[]> SLOT_POSITIONS = new EnumMap<>(EnumMechaSlotType.class);

    static
    {
        SLOT_POSITIONS.put(EnumMechaSlotType.LEGS, new int[] {84, 128});
        SLOT_POSITIONS.put(EnumMechaSlotType.HIPS, new int[] {60, 128});
        SLOT_POSITIONS.put(EnumMechaSlotType.LEFT_ARM, new int[] {36, 80});
        SLOT_POSITIONS.put(EnumMechaSlotType.LEFT_TOOL, new int[] {36, 56});
        SLOT_POSITIONS.put(EnumMechaSlotType.LEFT_SHOULDER, new int[] {60, 32});
        SLOT_POSITIONS.put(EnumMechaSlotType.HEAD, new int[] {84, 32});
        SLOT_POSITIONS.put(EnumMechaSlotType.FEET, new int[] {108, 128});
        SLOT_POSITIONS.put(EnumMechaSlotType.RIGHT_ARM, new int[] {132, 80});
        SLOT_POSITIONS.put(EnumMechaSlotType.RIGHT_TOOL, new int[] {132, 56});
        SLOT_POSITIONS.put(EnumMechaSlotType.RIGHT_SHOULDER, new int[] {108, 32});
        SLOT_POSITIONS.put(EnumMechaSlotType.U1, new int[] {10, 32});
        SLOT_POSITIONS.put(EnumMechaSlotType.U2, new int[] {10, 56});
        SLOT_POSITIONS.put(EnumMechaSlotType.U3, new int[] {10, 80});
        SLOT_POSITIONS.put(EnumMechaSlotType.U4, new int[] {10, 104});
        SLOT_POSITIONS.put(EnumMechaSlotType.U5, new int[] {10, 128});
    }

    @Nullable @Getter private final Mecha mecha;
    private final Container mechaInventory;
    private final DriveableMappedSlot[] cargoSlots = new DriveableMappedSlot[GRID_SLOT_COUNT];
    @Getter private int scrollRow;
    private final int mechaSlotEnd;
    private final int playerInventoryStart;
    private final int playerInventoryEnd;

    public static MechaInventoryMenu createFromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buffer)
    {
        Entity entity = inventory.player.level().getEntity(buffer.readVarInt());
        return new MechaInventoryMenu(containerId, inventory, entity instanceof Mecha found ? found : null);
    }

    public MechaInventoryMenu(int containerId, Inventory playerInventory, @Nullable Mecha mecha)
    {
        super(FlansMod.mechaInventoryMenu.get(), containerId);
        this.mecha = mecha;
        mechaInventory = mecha == null || mecha.getDriveableData() == null
            ? new SimpleContainer(1) : mecha.getDriveableData();

        for (int row = 0; row < VISIBLE_ROWS; row++)
        {
            for (int column = 0; column < COLUMN_COUNT; column++)
            {
                DriveableMappedSlot slot = new DriveableMappedSlot(mechaInventory,
                    CARGO_LEFT + column * SLOT_SIZE, CARGO_TOP + row * CARGO_ROW_HEIGHT, stack -> true);
                cargoSlots[row * COLUMN_COUNT + column] = slot;
                addSlot(slot);
            }
        }

        int mechaStart = mechaInventory instanceof DriveableData data ? data.getMechaInventoryStart() : -1;
        for (EnumMechaSlotType slotType : EnumMechaSlotType.values())
        {
            int[] position = SLOT_POSITIONS.get(slotType);
            if (position == null)
                continue;
            DriveableMappedSlot slot = new DriveableMappedSlot(mechaInventory, position[0], position[1], stack -> true);
            slot.mapTo(mechaStart < 0 ? -1 : mechaStart + slotType.ordinal());
            addSlot(slot);
        }
        mechaSlotEnd = slots.size();

        playerInventoryStart = slots.size();
        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                    PLAYER_INVENTORY_LEFT + column * SLOT_SIZE, 98 + row * SLOT_SIZE));
        }
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, PLAYER_INVENTORY_LEFT + column * SLOT_SIZE, 156));
        playerInventoryEnd = slots.size();

        remapCargoSlots();
    }

    public int getEntityId()
    {
        return mecha == null ? -1 : mecha.getId();
    }

    public int getCargoSlotCount()
    {
        return mechaInventory instanceof DriveableData data ? data.getNumCargoSlots() : 0;
    }

    public int getMaxScrollRow()
    {
        int rows = (getCargoSlotCount() + COLUMN_COUNT - 1) / COLUMN_COUNT;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id)
    {
        if (!stillValid(player))
            return false;
        if (id == SCROLL_UP_BUTTON && scrollRow > 0)
        {
            --scrollRow;
            remapCargoSlots();
            return true;
        }
        if (id == SCROLL_DOWN_BUTTON && scrollRow < getMaxScrollRow())
        {
            ++scrollRow;
            remapCargoSlots();
            return true;
        }
        if (id == DRIVEABLE_MENU_BUTTON)
        {
            if (mecha != null && player instanceof ServerPlayer serverPlayer)
                mecha.openDriveableInventoryMenu(serverPlayer);
            return true;
        }
        return false;
    }

    private void remapCargoSlots()
    {
        int start = mechaInventory instanceof DriveableData data ? data.getCargoInventoryStart() : -1;
        int count = getCargoSlotCount();
        for (int visible = 0; visible < cargoSlots.length; visible++)
        {
            int relative = scrollRow * COLUMN_COUNT + visible;
            cargoSlots[visible].mapTo(start < 0 || relative >= count ? -1 : start + relative);
        }
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index)
    {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack original = slotStack.copy();

        if (index < playerInventoryStart)
        {
            if (!moveItemStackTo(slotStack, playerInventoryStart, playerInventoryEnd, true))
                return ItemStack.EMPTY;
        }
        else if (!moveItemStackTo(slotStack, 0, mechaSlotEnd, false))
        {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();
        if (slotStack.getCount() == original.getCount())
            return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return original;
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return mecha != null && mecha.isAlive() && mecha.canPlayerAccessInventory(player);
    }
}
