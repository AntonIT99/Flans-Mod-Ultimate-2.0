package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.PaintableType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
        appendHoverText(tooltipComponents);

        String paintjobName = getPaintjob(stack).getDisplayName();
        if (!paintjobName.isEmpty())
            tooltipComponents.add(Component.literal(paintjobName).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));

        if (configType.getShootDelayMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Rate of Fire x" + Mth.floor(100.0F / configType.getShootDelayMultiplier()) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getDamageMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Damage x" + Mth.floor(configType.getDamageMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getRecoilMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Recoil x" + Mth.floor(configType.getRecoilMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getSpreadMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Bullet Spread x" + Mth.floor(configType.getSpreadMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getReloadTimeMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Reload Time x" + Mth.floor(configType.getReloadTimeMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.getBulletSpeedMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Projectile Speed x" + Mth.floor(configType.getBulletSpeedMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if (configType.isSilencer())
            tooltipComponents.add(Component.literal("Silenced").withStyle(ChatFormatting.BLUE));

        if (configType.getMeleeDamageMultiplier() != 1.0f)
            tooltipComponents.add(Component.literal("Melee Damage x" + Mth.floor(configType.getMeleeDamageMultiplier() * 100.0f) + "%").withStyle(ChatFormatting.BLUE));

        if(configType.isFlashlight())
            tooltipComponents.add(Component.literal("Flashlight " + configType.getFlashlightStrength()).withStyle(ChatFormatting.BLUE));
    }
}
