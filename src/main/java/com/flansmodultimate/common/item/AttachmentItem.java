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

        if (configType.getShootDelayMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Rate of Fire x" + IFlanItem.formatFloat(100.0F / configType.getShootDelayMultiplier()) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getDamageMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Damage x" + IFlanItem.formatFloat(configType.getDamageMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getRecoilMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Recoil x" + IFlanItem.formatFloat(configType.getRecoilMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getSpreadMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Bullet Spread x" + IFlanItem.formatFloat(configType.getSpreadMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getReloadTimeMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Reload Time x" + IFlanItem.formatFloat(configType.getReloadTimeMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getBulletSpeedMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Projectile Speed x" + IFlanItem.formatFloat(configType.getBulletSpeedMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.isSilencer())
            tooltipComponents.add(Component.literal("Silenced").withStyle(ChatFormatting.BLUE));

        if (configType.getMeleeDamageMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Melee Damage x" + IFlanItem.formatFloat(configType.getMeleeDamageMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if(configType.isFlashlight())
            tooltipComponents.add(Component.literal("Flashlight " + configType.getFlashlightStrength()).withStyle(ChatFormatting.BLUE));
    }
}
