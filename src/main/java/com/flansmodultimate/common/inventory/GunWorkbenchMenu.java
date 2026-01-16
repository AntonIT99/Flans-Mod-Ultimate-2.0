package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.AttachmentItem;
import com.flansmodultimate.common.item.GunItem;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * slot 0: gun
 * slots 1..8: specific attachment slots (barrel/scope/stock/grip/gadget/slide/pump/accessory)
 * slots 9..16: generic attachment slots (up to 8)
 */
public class GunWorkbenchMenu extends AbstractContainerMenu
{
    @Getter
    private final GunWorkbenchContainer gunInv;
    private final ContainerLevelAccess access;

    // layout constants
    private static final int GUN_SLOT_X = 184;
    private static final int GUN_SLOT_Y = 37;
    private static final int ATTACH_ROW_X = 16;
    private static final int SPECIFIC_ATTACH_ROW_Y = 89;
    private static final int GENERIC_ATTACH_ROW_Y = 115;
    private static final int SLOT_SIZE = 18;

    // counts for easier quickMoveStack ranges
    private static final int SPECIFIC_ATTACHMENT_SLOTS = 8;
    private static final int GENERIC_ATTACHMENT_SLOTS = 8;

    // menu slot indices
    private static final int SLOT_GUN = 0;
    private static final int SLOT_SPECIFIC_START = 1;
    private static final int SLOT_GENERIC_START = SLOT_SPECIFIC_START + SPECIFIC_ATTACHMENT_SLOTS;
    private static final int SLOT_MENU_END = SLOT_GENERIC_START + GENERIC_ATTACHMENT_SLOTS;

    // player inventory indices
    private final int playerInvStart;
    private final int playerInvEnd;

    private ItemStack lastGunStack = ItemStack.EMPTY;
    private boolean busy = false;

    private static final String[] SPECIFIC_TAGS = { GunItem.NBT_BARREL, GunItem.NBT_SCOPE, GunItem.NBT_STOCK, GunItem.NBT_GRIP, GunItem.NBT_GADGET, GunItem.NBT_SLIDE, GunItem.NBT_PUMP, GunItem.NBT_ACCESSORY };

    public GunWorkbenchMenu(int id, Inventory playerInv, ContainerLevelAccess access)
    {
        super(FlansMod.gunWorkbenchMenu.get(), id);
        this.access = access;

        // 1 gun + 8 specific + 8 generic = 17 slots
        gunInv = new GunWorkbenchContainer(this, 1 + SPECIFIC_ATTACHMENT_SLOTS + GENERIC_ATTACHMENT_SLOTS);

        // Slot 0: gun input
        addSlot(new GunWorkbenchSlot(this, gunInv, SLOT_GUN, GUN_SLOT_X, GUN_SLOT_Y));

        // Slots 1..8: specific attachments
        for (int i = 0; i < SPECIFIC_ATTACHMENT_SLOTS; i++)
            addSlot(new GunWorkbenchSlot(this, gunInv, SLOT_SPECIFIC_START + i, ATTACH_ROW_X + i * SLOT_SIZE, SPECIFIC_ATTACH_ROW_Y));

        // Slots 9..16: generic attachments
        for (int i = 0; i < GENERIC_ATTACHMENT_SLOTS; i++)
            addSlot(new GunWorkbenchSlot(this, gunInv, SLOT_GENERIC_START + i, ATTACH_ROW_X + i * SLOT_SIZE, GENERIC_ATTACH_ROW_Y));

        this.playerInvStart = this.slots.size();

        final int invX = 8;
        final int invY = 154;

        // Main inventory (3 rows x 9)
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                int x = invX + col * SLOT_SIZE;
                int y = invY + row * SLOT_SIZE;
                addSlot(new Slot(playerInv, col + row * 9 + 9, x, y));
            }
        }

        // Hotbar (1 row x 9)
        final int hotbarY = invY + 3 * SLOT_SIZE + 4;
        for (int col = 0; col < 9; col++)
        {
            int x = invX + col * SLOT_SIZE;
            addSlot(new Slot(playerInv, col, x, hotbarY));
        }

        this.playerInvEnd = this.slots.size();
    }

    /**
     * Shift-click logic.
     * <p>
     * Rules:
     * - From menu slots -> player inventory
     * - From player inventory:
     *    - If gun slot empty and item is gun -> move to gun slot
     *    - Else if item is attachment -> try specific slots first, then generic
     */
    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index)
    {
        Slot from = slots.get(index);
        if (!from.hasItem())
            return ItemStack.EMPTY;

        ItemStack fromStack = from.getItem();
        ItemStack copy = fromStack.copy();

        boolean fromMenu = index < SLOT_MENU_END;
        boolean fromPlayer = index >= playerInvStart && index < playerInvEnd;

        if (fromMenu)
        {
            if (index == SLOT_GUN)
                finalizeGunAndConsumeAttachments(fromStack);

            // move menu -> player
            if (!moveItemStackTo(fromStack, playerInvStart, playerInvEnd, true))
                return ItemStack.EMPTY;

            from.setChanged();
            return copy;
        }

        if (fromPlayer)
        {
            // 1) gun into gun slot if possible
            if (isGun(fromStack))
            {
                Slot gunSlot = slots.get(SLOT_GUN);
                if (!gunSlot.hasItem())
                {
                    if (moveItemStackTo(fromStack, SLOT_GUN, SLOT_GUN + 1, false))
                    {
                        from.setChanged();
                        return copy;
                    }
                }
            }

            // 2) attachments into attachment slots
            if (isAttachment(fromStack))
            {
                // try specific slots first
                if (moveItemStackTo(fromStack, SLOT_SPECIFIC_START, SLOT_SPECIFIC_START + SPECIFIC_ATTACHMENT_SLOTS, false))
                {
                    from.setChanged();
                    return copy;
                }

                // then generic
                if (moveItemStackTo(fromStack, SLOT_GENERIC_START, SLOT_MENU_END, false))
                {
                    from.setChanged();
                    return copy;
                }
            }

            // 3) otherwise do normal inventory swap: main <-> hotbar
            int hotbarStart = playerInvEnd - 9;
            if (index < hotbarStart)
            {
                if (!moveItemStackTo(fromStack, hotbarStart, playerInvEnd, false))
                    return ItemStack.EMPTY;
            }
            else
            {
                if (!moveItemStackTo(fromStack, playerInvStart, hotbarStart, false))
                    return ItemStack.EMPTY;
            }

            from.setChanged();
            return copy;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return access.evaluate((level, pos) -> {
            Block block = level.getBlockState(pos).getBlock();
            return block == FlansMod.gunWorkbench.get() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    @Override
    public void removed(@NotNull Player player)
    {
        super.removed(player);
        access.execute((level, pos) -> this.clearContainer(player, this.gunInv));
    }

    private static boolean isGun(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof GunItem;
    }

    private static boolean isAttachment(ItemStack stack)
    {
        // Replace with your real attachment base class / tag checks
        return !stack.isEmpty() && stack.getItem() instanceof AttachmentItem;
    }

    public ItemStack getGunStack()
    {
        return slots.get(0).getItem();
    }

    public boolean isAttachmentSlotEmpty(int attachmentSlotIndex)
    {
        return slots.get(1 + attachmentSlotIndex).getItem().isEmpty();
    }

    public void onWorkbenchChanged(Container inv)
    {
        if (busy)
            return;

        ItemStack gunStack = inv.getItem(SLOT_GUN);
        if (gunStack.isEmpty() || !(gunStack.getItem() instanceof GunItem))
            return;

        // Detect gun change (replacement)
        boolean gunChanged = lastGunStack.isEmpty()
            || !ItemStack.isSameItemSameTags(gunStack, lastGunStack);

        if (gunChanged)
        {
            // Load attachments from gun NBT into slots
            busy = true;
            loadAttachmentsFromGunIntoSlots(gunStack);
            busy = false;
        }
        else
        {
            // Attachments changed -> write back into gun NBT
            writeSlotsIntoGunNbt(gunStack);
        }

        lastGunStack = gunStack.copy();
    }

    private void loadAttachmentsFromGunIntoSlots(ItemStack gunStack)
    {
        var gunTag = gunStack.getOrCreateTag();
        var attachments = gunTag.getCompound("attachments");

        // specific 1..8
        for (int i = 0; i < SPECIFIC_ATTACHMENT_SLOTS; i++)
        {
            var attTag = attachments.getCompound(SPECIFIC_TAGS[i]);
            ItemStack att = attTag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(attTag);
            gunInv.setItem(SLOT_SPECIFIC_START + i, att);
        }

        // generic 9..16
        for (int i = 0; i < GENERIC_ATTACHMENT_SLOTS; i++)
        {
            var attTag = attachments.getCompound("generic_" + i);
            ItemStack att = attTag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(attTag);
            gunInv.setItem(SLOT_GENERIC_START + i, att);
        }

        gunInv.setChanged();
    }

    private void writeSlotsIntoGunNbt(ItemStack gunStack)
    {
        var gunTagOld = gunStack.getOrCreateTag();

        // 1.7.10 rebuilt the tag and preserved ammo/paint.
        // In modern, it's usually safer to KEEP existing tag and only replace attachments.
        var attachments = new net.minecraft.nbt.CompoundTag();

        // specific
        for (int i = 0; i < SPECIFIC_ATTACHMENT_SLOTS; i++)
        {
            writeAttachmentTag(attachments, gunInv.getItem(SLOT_SPECIFIC_START + i), SPECIFIC_TAGS[i]);
        }

        // generic
        for (int i = 0; i < GENERIC_ATTACHMENT_SLOTS; i++)
        {
            writeAttachmentTag(attachments, gunInv.getItem(SLOT_GENERIC_START + i), GunItem.NBT_GENERIC + i);
        }

        gunTagOld.put("attachments", attachments);
        gunStack.setTag(gunTagOld);
    }

    private static void writeAttachmentTag(CompoundTag attachments, ItemStack stack, String name)
    {
        CompoundTag t = new CompoundTag();
        if (!stack.isEmpty())
            stack.save(t);
        attachments.put(name, t);
    }

    public void finalizeGunAndConsumeAttachments(ItemStack gunStack)
    {
        // ensure gun NBT matches current slots
        writeSlotsIntoGunNbt(gunStack);

        // consume: clear slots 1..16 so they don't remain in the table
        busy = true;
        for (int i = SLOT_SPECIFIC_START; i < SLOT_MENU_END; i++)
            gunInv.setItem(i, ItemStack.EMPTY);
        busy = false;

        gunInv.setChanged();
    }
}