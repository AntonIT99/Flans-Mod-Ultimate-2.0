package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.PaintableType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class AttachmentItem extends Item implements IPaintableItem<AttachmentType>
{
    @Getter
    protected final AttachmentType configType;

    public AttachmentItem(AttachmentType configconfigType)
    {
        super(new Item.Properties().stacksTo(1));
        this.configType = configconfigType;
    }

    @Override
    public PaintableType getPaintableType()
    {
        return configType;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.empty());

        if (configType.getShootDelayMultiplier() != 1F && configType.getShootDelayMultiplier() != 0F)
            tooltipComponents.add(IFlanItem.modifierLine("Fire Rate", 1F / configType.getShootDelayMultiplier(), false));

        if (configType.getDamageMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine("Damage", configType.getDamageMultiplier(), false));

        if (configType.getRecoilMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine("Recoil", configType.getRecoilMultiplier(), true));

        if (configType.getSpreadMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine("Bullet Spread", configType.getSpreadMultiplier(), true));

        if (configType.getReloadTimeMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine("Reload Time", configType.getReloadTimeMultiplier(), true));

        if (configType.getBulletSpeedMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine("Bullet Speed", configType.getBulletSpeedMultiplier(), false));

        if (configType.getMeleeDamageMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine("Melee Damage", configType.getMeleeDamageMultiplier(), false));

        if (configType.isSilencer())
            tooltipComponents.add(Component.literal("Silenced").withStyle(ChatFormatting.DARK_GREEN));

        if(configType.isFlashlight())
            tooltipComponents.add(Component.literal("Flashlight " + configType.getFlashlightStrength()).withStyle(ChatFormatting.DARK_GREEN));
    }
}
