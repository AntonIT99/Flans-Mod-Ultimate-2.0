package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfigs;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public interface IFlanItem<T extends InfoType> extends ItemLike
{
    T getConfigType();

    default String getContentPack()
    {
        return FilenameUtils.getBaseName(getConfigType().getContentPack().getName());
    }

    default void appendContentPackNameAndItemDescription(@NotNull ItemStack stack, @NotNull List<Component> tooltipComponents)
    {
        if (BooleanUtils.isTrue(ModClientConfigs.showPackNameInItemDescriptions.get()) && !getContentPack().isBlank())
            tooltipComponents.add(Component.literal(getContentPack()).withStyle(ChatFormatting.DARK_GRAY));

        if (!Screen.hasShiftDown())
        {
            for (String line : getConfigType().getDescription().split("_"))
            {
                if (!line.isBlank())
                    tooltipComponents.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }

        tooltipComponents.add(Component.empty());
    }

    /**
     * Helper to render "BlueLabel: gray value"
     */
    static Component statLine(String label, String value)
    {
        return Component.literal(label)
                .withStyle(ChatFormatting.BLUE)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(value).withStyle(ChatFormatting.GRAY));
    }

    /**
     * Slightly indented stat line for sub-values (vs Living / vs Player / etc.)
     */
    static Component indentedStatLine(String label, String value)
    {
        return Component.literal("  " + label)
                .withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(value).withStyle(ChatFormatting.GRAY));
    }

    /**
     * Format floats nicely (no trailing .0 if not needed)
     */
    static String formatFloat(float f)
    {
        if (Math.abs(f - Math.round(f)) < 0.0001F)
        {
            return Integer.toString(Math.round(f));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", f);
    }

    /**
     * Format doubles nicely (no trailing .0 if not needed)
     */
    static String formatDouble(double d)
    {
        if (Math.abs(d - Math.round(d)) < 0.0001)
        {
            return Long.toString(Math.round(d));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", d);
    }

    /**
     * Adds explosion damage stats avoiding redundant lines.
     */
    static void appendDamageStats(List<Component> tooltip, DamageStats damageStats, String labelBaseName)
    {
        final float EPS = 0.0001f;

        // Always show base explosion damage if it's meaningful
        tooltip.add(IFlanItem.statLine(labelBaseName, formatFloat(damageStats.getDamage())));

        // vs Living: only show if explicitly configured AND different from base
        if (damageStats.isReadDamageVsLiving() && Math.abs(damageStats.getDamageVsLiving() - damageStats.getDamage()) > EPS)
            tooltip.add(IFlanItem.indentedStatLine("vs Living", formatFloat(damageStats.getDamageVsLiving())));

        // vs Player: inherits from vsLiving
        if (damageStats.isReadDamageVsPlayer() && Math.abs(damageStats.getDamageVsPlayer() - damageStats.getDamageVsLiving()) > EPS)
            tooltip.add(IFlanItem.indentedStatLine("vs Players", formatFloat(damageStats.getDamageVsPlayer())));

        // vs Vehicle: inherits from base
        if (damageStats.isReadDamageVsVehicles() && Math.abs(damageStats.getDamageVsVehicles() - damageStats.getDamage()) > EPS)
            tooltip.add(IFlanItem.indentedStatLine("vs Vehicles", formatFloat(damageStats.getDamageVsVehicles())));

        // vs Plane: inherits from vsVehicle
        if (damageStats.isReadDamageVsPlanes() && Math.abs(damageStats.getDamageVsPlanes() - damageStats.getDamageVsVehicles()) > EPS)
            tooltip.add(IFlanItem.indentedStatLine("vs Planes", formatFloat(damageStats.getDamageVsPlanes())));
    }
}
