package com.flansmodultimate.common.item;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.hooks.ClientHooks;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class BulletItem extends ShootableItem implements IFlanItem<BulletType>
{
    @Getter
    protected final BulletType configType;
    @Setter
    protected String originGunbox = StringUtils.EMPTY;

    public BulletItem(BulletType configType)
    {
        super(configType);
        this.configType = configType;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        tooltipComponents.add(Component.empty());

        if (!ClientHooks.TOOLTIPS.isShiftDown())
        {
            Component keyName = ClientHooks.TOOLTIPS.getShiftKeyName().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
            tooltipComponents.add(Component.translatable("tooltip.flansmodultimate.hold_for_details", keyName).withStyle(ChatFormatting.GRAY));
        }
        else
        {
            super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

            if (configType.hasDifferentRounds())
            {
                tooltipComponents.add(Component.translatable("tooltip.flansmodultimate.rounds_mix").append(": ").withStyle(ChatFormatting.BLUE));
                configType.getPeriod().forEach(round ->
                    tooltipComponents.add(Component.literal("  " + round.name() + " (" + round.count() + ")").withStyle(ChatFormatting.DARK_AQUA)));
            }

            if (configType.hasDifferentRounds())
            {
                tooltipComponents.add(Component.translatable("tooltip.flansmodultimate.muzzle_velocity").append(":").withStyle(ChatFormatting.BLUE));
                configType.getPeriod().forEach(round ->
                    tooltipComponents.add(Component.literal("  " + round.name() + " " + IFlanItem.formatFloat(round.stats().bulletSpeed() * 20F) + " m/s").withStyle(ChatFormatting.GRAY)));
            }
            else if (configType.getBulletSpeed() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Muzzle Velocity", IFlanItem.formatFloat(configType.getBulletSpeed() * 20F, 3) + " m/s"));

            if (configType.hasDifferentRounds() && configType.getPeriod().stream().anyMatch(round -> round.stats().penetrationAt100m() > 0F))
            {
                tooltipComponents.add(Component.translatable("tooltip.flansmodultimate.penetration_at_100m").append(":").withStyle(ChatFormatting.BLUE));
                configType.getPeriod().forEach(round -> {
                    if (round.stats().penetrationAt100m() > 0)
                        tooltipComponents.add(Component.literal("  " + round.name() + " " + IFlanItem.formatFloat(round.stats().penetrationAt100m()) + "mm").withStyle(ChatFormatting.GRAY));
                });
            }
            else if (configType.getBulletSpeed() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Penetration At 100m", IFlanItem.formatFloat(configType.getPenetrationAt100m()) + "mm"));

            tooltipComponents.add(IFlanItem.statLine("Penetrating Power", IFlanItem.formatFloat(configType.getPenetratingPower())));

            if (hasLockOn())
                tooltipComponents.add(IFlanItem.statLine(Component.translatable("tooltip.flansmodultimate.guidance"), Component.translatable("tooltip.flansmodultimate.guidance.lock_on")));
            else if (configType.isManualGuidance())
                tooltipComponents.add(IFlanItem.statLine(Component.translatable("tooltip.flansmodultimate.guidance"), Component.translatable("tooltip.flansmodultimate.guidance.manual")));
            else if (configType.isLaserGuidance())
                tooltipComponents.add(IFlanItem.statLine(Component.translatable("tooltip.flansmodultimate.guidance"), Component.translatable("tooltip.flansmodultimate.guidance.laser")));
            else if (configType.getWeaponType() == EnumWeaponType.MISSILE)
                tooltipComponents.add(IFlanItem.statLine(Component.translatable("tooltip.flansmodultimate.guidance"), Component.translatable("tooltip.flansmodultimate.guidance.unguided")));

            if (hasLockOn() || configType.isLaserGuidance())
            {
                tooltipComponents.add(IFlanItem.statLine("Turning Force", IFlanItem.formatFloat(configType.getLockOnForce() * 10F) + "G"));
            }

            if (StringUtils.isNotBlank(originGunbox))
                tooltipComponents.add(IFlanItem.statLine("Box", originGunbox));
        }
    }

    private boolean hasLockOn()
    {
        return configType.isLockOnToLivings() || configType.isLockOnToMechas() || configType.isLockOnToPlanes() || configType.isLockOnToPlayers() || configType.isLockOnToVehicles();
    }
}
