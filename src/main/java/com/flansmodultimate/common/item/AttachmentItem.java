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

        if (configType.isSilencer())
            tooltipComponents.add(Component.translatable(TooltipKeys.SILENCED).withStyle(ChatFormatting.DARK_GREEN));

        if (configType.getShootDelayMultiplier() != 1F && configType.getShootDelayMultiplier() != 0F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.FIRE_RATE), 1F / configType.getShootDelayMultiplier(), false));

        if (configType.getDamageMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.DAMAGE), configType.getDamageMultiplier(), false));

        if (configType.getRecoilMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.RECOIL), configType.getRecoilMultiplier(), true));

        if (configType.getSpreadMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.BULLET_SPREAD), configType.getSpreadMultiplier(), true));

        if (configType.getReloadTimeMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.RELOAD_TIME), configType.getReloadTimeMultiplier(), true));

        if (configType.getBulletSpeedMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.BULLET_SPEED), configType.getBulletSpeedMultiplier(), false));

        if (configType.getMeleeDamageMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.MELEE_DAMAGE), configType.getMeleeDamageMultiplier(), false));

        if (configType.getMoveSpeedMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.MOVE_SPEED), configType.getMoveSpeedMultiplier(), false));

        if (configType.isFlashlight())
        {
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.FLASHLIGHT_STRENGTH), String.valueOf(configType.getFlashlightStrength())));
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.FLASHLIGHT_RANGE), String.valueOf(configType.getFlashlightRange())));
        }

        float zoomFactor = Math.max(configType.getZoomFactor(), configType.getFovFactor());
        if (zoomFactor != 1F)
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.ZOOM_FACTOR), "x" + IFlanItem.formatFloat(zoomFactor)));
    }
}
