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
    public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        appendContentPackNameAndItemDescription(stack, tooltipComponents);
        // FancyDescription false keeps the plain pack name and description, as in 1.7.10.
        if (!configType.isFancyDescription())
            return;
        tooltipComponents.add(Component.empty());

        if (!ClientHooks.TOOLTIPS.isShiftDown())
        {
            Component keyName = ClientHooks.TOOLTIPS.getShiftKeyName().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
            tooltipComponents.add(Component.translatable(TooltipKeys.HOLD_FOR_DETAILS, keyName).withStyle(ChatFormatting.GRAY));
        }
        else
        {
            super.appendHoverText(stack, context, tooltipComponents, isAdvanced);

            if (StringUtils.isNotBlank(originGunbox))
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.BOX), originGunbox));
        }
    }

    @Override
    public void appendAmmoStats(@NotNull List<Component> tooltipComponents, @Nullable AmmoStatContext context)
    {
        super.appendAmmoStats(tooltipComponents, context);

        List<RoundView> rounds = roundViews(configType, context);
        if (!rounds.isEmpty())
        {
            tooltipComponents.add(Component.translatable(TooltipKeys.ROUNDS_MIX).append(": ").withStyle(ChatFormatting.BLUE));
            rounds.forEach(round ->
                tooltipComponents.add(Component.literal("  " + round.name() + " (" + round.count() + ")").withStyle(ChatFormatting.DARK_AQUA)));
        }

        if (context == null || context.showLaunchStats())
            appendPerRound(tooltipComponents, TooltipKeys.MUZZLE_VELOCITY, rounds, shot -> {
            float velocity = muzzleVelocity(configType, shot, context);
            return velocity > 0F ? IFlanItem.formatFloat(velocity * 20F, rounds.isEmpty() ? 3 : 2) + " m/s" : null;
        });

        appendPerRound(tooltipComponents, TooltipKeys.PENETRATION_AT_100M, rounds, shot -> {
            float penetration = penetrationAt100m(configType, shot, context);
            return penetration > 0F ? IFlanItem.formatFloat(penetration) + "mm" : null;
        });

        // Kinetic penetrating power cannot be stated meaningfully until both mass and launch velocity are known.
        // In a standalone ammo tooltip there may be no weapon context to provide the missing velocity.
        boolean hasUnresolvedKineticPower = projectileMass(configType, 0, context) > 0F
            && muzzleVelocity(configType, 0, context) <= 0F;
        if (!hasUnresolvedKineticPower)
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.PENETRATING_POWER),
                IFlanItem.formatFloat(penetratingPower(configType, 0, context))));

        if (hasLockOn())
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.GUIDANCE), Component.translatable(TooltipKeys.GUIDANCE_LOCK_ON)));
        else if (configType.isManualGuidance())
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.GUIDANCE), Component.translatable(TooltipKeys.GUIDANCE_MANUAL)));
        else if (configType.isLaserGuidance())
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.GUIDANCE), Component.translatable(TooltipKeys.GUIDANCE_LASER)));
        else if (configType.getWeaponType() == EnumWeaponType.MISSILE)
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.GUIDANCE), Component.translatable(TooltipKeys.GUIDANCE_UNGUIDED)));

        if (hasLockOn() || configType.isLaserGuidance())
        {
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.TURNING_FORCE), IFlanItem.formatFloat(configType.getLockOnForce() * 10F) + "G"));
        }
    }

    private boolean hasLockOn()
    {
        return configType.isLockOnToLivings() || configType.isLockOnToMechas() || configType.isLockOnToPlanes() || configType.isLockOnToPlayers() || configType.isLockOnToVehicles();
    }
}
