package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.driveables.EnumWeaponType;
import com.wolffsarmormod.common.types.BulletType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
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
        appendHoverText(tooltipComponents);

        if (!Screen.hasShiftDown())
        {
            KeyMapping shiftKey = Minecraft.getInstance().options.keyShift;
            Component keyName = shiftKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);

            tooltipComponents.add(Component.literal("Hold ").append(keyName).append(" for details").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            if (!originGunbox.isBlank())
                tooltipComponents.add(IFlanItem.statLine("Box", originGunbox));

            tooltipComponents.add(IFlanItem.statLine("Damage", String.valueOf(configType.getDamageVsLiving())));
            if (configType.getDamageVsVehicles() != configType.getDamageVsLiving())
                tooltipComponents.add(IFlanItem.statLine("Damage Vs Vehicles", String.valueOf(configType.getDamageVsVehicles())));
            if (configType.getDamageVsPlanes() != configType.getDamageVsVehicles())
                tooltipComponents.add(IFlanItem.statLine("Damage Vs Planes", String.valueOf(configType.getDamageVsPlanes())));

            tooltipComponents.add(IFlanItem.statLine("Penetration", String.valueOf(configType.getPenetratingPower())));
            tooltipComponents.add(IFlanItem.statLine("Rounds", String.valueOf(configType.getRoundsPerItem())));
            tooltipComponents.add(IFlanItem.statLine("Fall Speed", String.valueOf(configType.getFallSpeed())));

            if (configType.getExplosionRadius() > 0F)
                tooltipComponents.add(IFlanItem.statLine("Explosion Radius", String.valueOf(configType.getExplosionRadius())));
            if (configType.getExplosionPower() > 1F)
                tooltipComponents.add(IFlanItem.statLine("Explosion Power", String.valueOf(configType.getExplosionPower())));

            if (configType.getNumBullets() > 1F)
                tooltipComponents.add(IFlanItem.statLine("Shot", String.valueOf(configType.getNumBullets())));
            if (configType.getBulletSpread() > 1F)
                tooltipComponents.add(IFlanItem.statLine("Shot", String.valueOf(configType.getBulletSpread())));

            if (configType.isLockOnToLivings() || configType.isLockOnToMechas() || configType.isLockOnToPlanes() || configType.isLockOnToPlayers() || configType.isLockOnToVehicles())
                tooltipComponents.add(IFlanItem.statLine("Guidance", "LockOn"));
            else if (configType.isManualGuidance())
                tooltipComponents.add(IFlanItem.statLine("Guidance", "Manual"));
            else if (configType.isLaserGuidance())
                tooltipComponents.add(IFlanItem.statLine("Guidance", "Laser"));
            else if (configType.getWeaponType() == EnumWeaponType.MISSILE)
                tooltipComponents.add(IFlanItem.statLine("Guidance", "Unguided"));

        }
    }
}
