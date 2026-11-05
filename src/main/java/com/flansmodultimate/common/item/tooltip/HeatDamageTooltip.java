package com.flansmodultimate.common.item.tooltip;

import com.flansmodultimate.common.driveables.armor.VehicleProjectileDamageResolver;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.config.ModCommonConfig;
import org.jetbrains.annotations.Nullable;

/** Shared HEAT jet range; impact and blast statistics remain separate. */
public final class HeatDamageTooltip
{
    public static final String NOTE = "tooltip.flansmodultimate.heat_damage_note";

    private HeatDamageTooltip() {}

    public static String range(BulletType ammunition, @Nullable FiredShot shot)
    {
        float charge = Driveable.shapedChargeKg(shot, ammunition);
        float low = VehicleProjectileDamageResolver.shapedChargeDamage(charge, 1F, ModCommonConfig.heatDamageReference());
        float high = VehicleProjectileDamageResolver.shapedChargeDamage(charge, 2F, ModCommonConfig.heatDamageReference());
        return IFlanItem.formatFloat(low, 1) + "\u2013" + IFlanItem.formatFloat(high, 1) + " HEAT*";
    }
}
