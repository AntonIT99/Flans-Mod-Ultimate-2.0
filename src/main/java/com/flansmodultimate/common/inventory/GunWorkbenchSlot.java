package com.flansmodultimate.common.inventory;

import com.flansmodultimate.common.guns.EnumAttachmentType;
import com.flansmodultimate.common.item.AttachmentItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GunWorkbenchSlot extends Slot
{
    /** 0 = gun input, 1..8 = specific slots, >=9 = generic slots */
    private final int slotId;
    private final GunWorkbenchMenu menu;

    public GunWorkbenchSlot(GunWorkbenchMenu menu, Container container, int slotId, int x, int y)
    {
        super(container, slotId, x, y);
        this.menu = menu;
        this.slotId = slotId;
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack stack)
    {
        if (stack.getItem() instanceof GunItem)
            menu.finalizeGunAndConsumeAttachments(stack);
        super.onTake(player, stack);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack)
    {
        if (stack.isEmpty())
            return false;

        return switch (slotId)
        {
            case 0 -> stack.getItem() instanceof GunItem gunItem && !gunItem.getConfigType().isDeployable() && stack.hasTag();
            case 1 -> isAttachmentOfType(stack, EnumAttachmentType.BARREL) && isSlotAllowedByGun(EnumAttachmentType.BARREL);
            case 2 -> isAttachmentOfType(stack, EnumAttachmentType.SIGHTS) && isSlotAllowedByGun(EnumAttachmentType.SIGHTS);
            case 3 -> isAttachmentOfType(stack, EnumAttachmentType.STOCK) && isSlotAllowedByGun(EnumAttachmentType.STOCK);
            case 4 -> isAttachmentOfType(stack, EnumAttachmentType.GRIP) && isSlotAllowedByGun(EnumAttachmentType.GRIP);
            case 5 -> isAttachmentOfType(stack, EnumAttachmentType.GADGET) && isSlotAllowedByGun(EnumAttachmentType.GADGET);
            case 6 -> isAttachmentOfType(stack, EnumAttachmentType.SLIDE) && isSlotAllowedByGun(EnumAttachmentType.SLIDE);
            case 7 -> isAttachmentOfType(stack, EnumAttachmentType.PUMP) && isSlotAllowedByGun(EnumAttachmentType.PUMP);
            case 8 -> isAttachmentOfType(stack, EnumAttachmentType.ACCESSORY) && isSlotAllowedByGun(EnumAttachmentType.ACCESSORY);
            default -> isAttachmentOfType(stack, EnumAttachmentType.GENERIC) && isSlotAllowedByGun(EnumAttachmentType.GENERIC);
        };
    }

    private boolean isSlotAllowedByGun(EnumAttachmentType t)
    {
        if (!(menu.getGunStack().getItem() instanceof GunItem gunItem))
            return false;

        GunType gunType = gunItem.getConfigType();

        return switch (t)
        {
            case BARREL -> gunType.isAllowBarrelAttachments();
            case SIGHTS -> gunType.isAllowScopeAttachments();
            case STOCK -> gunType.isAllowStockAttachments();
            case GRIP -> gunType.isAllowGripAttachments();
            case GADGET -> gunType.isAllowGadgetAttachments();
            case SLIDE -> gunType.isAllowSlideAttachments();
            case PUMP -> gunType.isAllowPumpAttachments();
            case ACCESSORY -> gunType.isAllowAccessoryAttachments();
            case GENERIC -> (slotId - 9) < gunType.getNumGenericAttachmentSlots();
        };
    }

    private boolean isAttachmentOfType(ItemStack stack, EnumAttachmentType wantedType)
    {
        if (!canAttachToCurrentGun(stack) || !(stack.getItem() instanceof AttachmentItem att))
            return false;

        return att.getConfigType().getEnumAttachmentType() == wantedType;
    }

    private boolean canAttachToCurrentGun(ItemStack stack)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof AttachmentItem))
            return false;

        ItemStack gunStack = menu.getGunStack();
        if (gunStack.isEmpty() || !(gunStack.getItem() instanceof GunItem gunItem))
            return false;

        AttachmentType attachmentType = ((AttachmentItem) stack.getItem()).getConfigType();
        GunType gunType = gunItem.getConfigType();

        return gunType.isAllowAllAttachments() || gunType.getAllowedAttachments().contains(attachmentType);
    }

    @Override
    public int getMaxStackSize()
    {
        return 1;
    }

    @Override
    public boolean mayPickup(@NotNull Player player)
    {
        return true;
    }
}