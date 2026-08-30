package com.flansmodultimate.common.item;

import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.PilotGun;
import com.flansmodultimate.common.driveables.ShootPoint;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders a compact summary of the primary and secondary weapon banks a
 * driveable actually fires with.
 *
 * <p>Every value is read back through the same {@link DriveableType} accessors
 * the firing code uses, so the tooltip cannot drift from what the driveable
 * does in play. That matters most for the shoot delay, which a pack may
 * delegate to the mounted gun via {@code ReadWeaponsFromGunTypes}.
 *
 * <p>Passenger gun mounts are deliberately left out; they belong to a seat
 * rather than to a driver weapon bank.
 */
public final class DriveableWeaponTooltip
{
    private static final String PREFIX = "tooltip.flansmodultimate.weapons.";
    private DriveableWeaponTooltip() {}

    public static void append(DriveableType type, List<Component> tooltip)
    {
        if (type == null)
            return;
        boolean hasPrimary = type.weaponType(false) != EnumWeaponType.NONE;
        boolean hasSecondary = type.weaponType(true) != EnumWeaponType.NONE;
        if (!hasPrimary && !hasSecondary)
            return;

        if (hasPrimary)
            appendBank(type, false, tooltip);
        if (hasSecondary)
            appendBank(type, true, tooltip);
    }

    /**
     * A bank is named by the guns it mounts when it has any, since those are what
     * the driveable actually fires, and by its weapon type otherwise.
     */
    private static void appendBank(DriveableType type, boolean secondary, List<Component> tooltip)
    {
        List<GunType> guns = mountedGuns(type, secondary);
        String name = guns.isEmpty()
            ? translate("type." + type.weaponType(secondary).name().toLowerCase(Locale.ROOT))
            : joinNames(guns.stream().map(GunType::getName).toList());
        tooltip.add(IFlanItem.statLine(Component.translatable(secondary ? TooltipKeys.WEAPONS_SECONDARY : TooltipKeys.WEAPONS_PRIMARY), name));

        float delay = type.shootDelay(secondary);
        if (delay > 0F)
            tooltip.add(IFlanItem.statLine(Component.translatable(secondary ? TooltipKeys.WEAPONS_SECONDARY_FIRE_RATE : TooltipKeys.WEAPONS_PRIMARY_FIRE_RATE),
                IFlanItem.formatFloat(1200F / delay, 1) + " rpm"));
    }

    /** The guns mounted on this bank's shoot points, in authored order, deduplicated. */
    private static List<GunType> mountedGuns(DriveableType type, boolean secondary)
    {
        List<GunType> guns = new ArrayList<>();
        for (ShootPoint point : type.shootPoints(secondary))
        {
            if (point.getRootPos() instanceof PilotGun pilotGun)
            {
                GunType gunType = pilotGun.getType();
                if (gunType != null && !guns.contains(gunType))
                    guns.add(gunType);
            }
        }
        return guns;
    }

    private static String joinNames(List<String> names)
    {
        return String.join(", ", names);
    }

    private static String translate(String key)
    {
        return Component.translatable(PREFIX + key).getString();
    }
}
