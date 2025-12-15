package com.flansmodultimate.common.item;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.types.BulletType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

        if (!Screen.hasShiftDown())
        {
            KeyMapping shiftKey = Minecraft.getInstance().options.keyShift;
            Component keyName = shiftKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
            tooltipComponents.add(Component.literal("Hold ").append(keyName).append(" for details").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

            if (StringUtils.isNotBlank(originGunbox))
                tooltipComponents.add(IFlanItem.statLine("Box", originGunbox));

            tooltipComponents.add(IFlanItem.statLine("Penetration", IFlanItem.formatFloat(configType.getPenetratingPower())));

            if (configType.getNumBullets() != 1)
                tooltipComponents.add(IFlanItem.statLine("Shot", String.valueOf(configType.getNumBullets())));

            if (hasLockOn())
                tooltipComponents.add(IFlanItem.statLine("Guidance", "LockOn"));
            else if (configType.isManualGuidance())
                tooltipComponents.add(IFlanItem.statLine("Guidance", "Manual"));
            else if (configType.isLaserGuidance())
                tooltipComponents.add(IFlanItem.statLine("Guidance", "Laser"));
            else if (configType.getWeaponType() == EnumWeaponType.MISSILE)
                tooltipComponents.add(IFlanItem.statLine("Guidance", "Unguided"));

            if (hasLockOn() || configType.isLaserGuidance())
            {
                tooltipComponents.add(IFlanItem.statLine("Turning Force", IFlanItem.formatFloat(configType.getLockOnForce() * 10F) + "G"));
            }
        }
    }

    private boolean hasLockOn()
    {
        return configType.isLockOnToLivings() || configType.isLockOnToMechas() || configType.isLockOnToPlanes() || configType.isLockOnToPlayers() || configType.isLockOnToVehicles();
    }
}
