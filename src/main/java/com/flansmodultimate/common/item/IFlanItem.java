package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfig;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.UUID;

public interface IFlanItem<T extends InfoType> extends ItemLike
{
    T getConfigType();

    default String getContentPack()
    {
        return FilenameUtils.getBaseName(getConfigType().getContentPack().getName());
    }

    default void appendContentPackNameAndItemDescription(@NotNull ItemStack stack, @NotNull List<Component> tooltipComponents)
    {
        if (ModClientConfig.get().showPackNameInItemDescriptions && !getContentPack().isBlank())
            tooltipComponents.add(Component.literal(getContentPack()).withStyle(ChatFormatting.DARK_GRAY));

        if (!Screen.hasShiftDown())
        {
            for (String line : getConfigType().getDescription().split("_"))
            {
                if (!line.isBlank())
                    tooltipComponents.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
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

    /**
     * Helper to render "BlueLabel: gray value"
     */
    static MutableComponent statLine(String label, String value)
    {
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.BLUE)
                .append(Component.literal(value).withStyle(ChatFormatting.GRAY));
    }

    /**
     * Slightly indented stat line for sub-values (vs Living / vs Player / etc.)
     */
    static MutableComponent indentedStatLine(String label, String value)
    {
        return Component.literal("  " + label + ": ")
                .withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(value).withStyle(ChatFormatting.GRAY));
    }

    static MutableComponent modifierLine(String label, float value, boolean invertColor)
    {
        float deltaPercent = (value - 1F) * 100F;
        ChatFormatting color = ((deltaPercent >= 0F && !invertColor) || (deltaPercent < 0F && invertColor)) ? ChatFormatting.GREEN : ChatFormatting.RED;
        String sign = deltaPercent > 0F ? "+" : "";
        return Component.literal(sign + IFlanItem.formatFloat(deltaPercent) + "% " + label).withStyle(color);
    }

    DecimalFormat floatFormat = initFloatFormat();

    private static DecimalFormat initFloatFormat()
    {
        DecimalFormat df = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(java.util.Locale.ROOT));
        df.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return df;
    }

    /**
     * Format floats nicely (no trailing .0 if not needed)
     */
    static String formatFloat(float f)
    {
        return floatFormat.format(f);
    }

    /**
     * Format doubles nicely (no trailing .0 if not needed)
     */
    static String formatDouble(double d)
    {
        return floatFormat.format(d);
    }

    static UUID getOrCreateStackUUID(ItemStack stack, String key)
    {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(key))
            tag.putUUID(key, UUID.randomUUID());
        return tag.getUUID(key);
    }
}
