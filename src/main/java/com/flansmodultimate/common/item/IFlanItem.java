package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.hooks.ClientHooks;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

        if (!ClientHooks.TOOLTIPS.isShiftDown())
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
    static void appendDamageStats(List<Component> tooltip, DamageStats damageStats, String labelBaseKey)
    {
        final float EPS = 0.0001f;

        // Always show base explosion damage if it's meaningful
        tooltip.add(IFlanItem.statLine(Component.translatable(labelBaseKey), formatFloat(damageStats.getDamage(), 1)));

        // vs Living: only show if explicitly configured AND different from base
        if (damageStats.isReadDamageVsLiving() && Math.abs(damageStats.getDamageVsLiving() - damageStats.getDamage()) > EPS)
            tooltip.add(IFlanItem.indentedStatLine(Component.translatable(TooltipKeys.VS_LIVING), formatFloat(damageStats.getDamageVsLiving(), 1)));

        // vs Player: inherits from vsLiving
        if (damageStats.isReadDamageVsPlayer() && Math.abs(damageStats.getDamageVsPlayer() - damageStats.getDamageVsLiving()) > EPS)
            tooltip.add(IFlanItem.indentedStatLine(Component.translatable(TooltipKeys.VS_PLAYERS), formatFloat(damageStats.getDamageVsPlayer(), 1)));

        // vs Vehicle: inherits from base
        if (damageStats.isReadDamageVsVehicles() && Math.abs(damageStats.getDamageVsVehicles() - damageStats.getDamage()) > EPS)
            tooltip.add(IFlanItem.indentedStatLine(Component.translatable(TooltipKeys.VS_VEHICLES), formatFloat(damageStats.getDamageVsVehicles(), 1)));

        // vs Plane: inherits from vsVehicle
        if (damageStats.isReadDamageVsPlanes() && Math.abs(damageStats.getDamageVsPlanes() - damageStats.getDamageVsVehicles()) > EPS)
            tooltip.add(IFlanItem.indentedStatLine(Component.translatable(TooltipKeys.VS_PLANES), formatFloat(damageStats.getDamageVsPlanes(), 1)));
    }

    /**
     * Helper to render "BlueLabel: gray value", label localized via a translation key.
     */
    static MutableComponent statLine(Component label, String value)
    {
        return Component.empty()
            .append(label.copy().withStyle(ChatFormatting.BLUE))
            .append(Component.literal(": ").withStyle(ChatFormatting.BLUE))
            .append(Component.literal(value).withStyle(ChatFormatting.GRAY));
    }

    static MutableComponent statLine(Component label, Component value)
    {
        return Component.empty()
            .append(label.copy().withStyle(ChatFormatting.BLUE))
            .append(Component.literal(": ").withStyle(ChatFormatting.BLUE))
            .append(value.copy().withStyle(ChatFormatting.GRAY));
    }

    /** Indented stat line whose label is localized via a translation key. */
    static MutableComponent indentedStatLine(Component label, String value)
    {
        return Component.literal("  ").withStyle(ChatFormatting.DARK_AQUA)
            .append(label.copy().withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal(": ").withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal(value).withStyle(ChatFormatting.GRAY));
    }

    /**
     * Indented stat line for sub-values whose label is already resolved text
     * (e.g. an item's localized display name), not a translation key.
     */
    static MutableComponent indentedStatLine(String label, String value)
    {
        return Component.literal("  " + label + ": ")
            .withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal(value).withStyle(ChatFormatting.GRAY));
    }

    /** Modifier line whose label is localized via a translation key. */
    static MutableComponent modifierLine(Component label, float value, boolean invertColor)
    {
        float deltaPercent = (value - 1F) * 100F;
        ChatFormatting color = ((deltaPercent >= 0F && !invertColor) || (deltaPercent < 0F && invertColor)) ? ChatFormatting.GREEN : ChatFormatting.RED;
        String sign = deltaPercent > 0F ? "+" : "";
        return Component.literal(sign + IFlanItem.formatFloat(deltaPercent) + "% ").withStyle(color)
            .append(label.copy().withStyle(color));
    }

    ThreadLocal<Map<Integer, DecimalFormat>> UP_TO_CACHE = ThreadLocal.withInitial(HashMap::new);

    private static DecimalFormat decimalFormatUpTo(int decimals)
    {
        if (decimals < 0)
            throw new IllegalArgumentException("decimals < 0");

        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
        df.setGroupingUsed(false);
        df.setRoundingMode(RoundingMode.HALF_UP);

        df.setMinimumFractionDigits(0);
        df.setMaximumFractionDigits(decimals);
        return df;
    }

    /**
     * Format floats nicely (no trailing zeros, up to `decimals`).
     */
    static String formatFloat(float f, int decimals)
    {
        if (decimals < 0)
            throw new IllegalArgumentException("decimals < 0");

        DecimalFormat fmt = UP_TO_CACHE.get()
            .computeIfAbsent(decimals, IFlanItem::decimalFormatUpTo);

        return fmt.format(f);
    }

    static String formatFloat(float f)
    {
        return formatFloat(f, 2);
    }

    /**
     * Format doubles nicely (no trailing .0 if not needed)
     */
    static String formatDouble(double d, int decimals)
    {
        if (decimals < 0)
            throw new IllegalArgumentException("decimals < 0");

        DecimalFormat fmt = UP_TO_CACHE.get()
            .computeIfAbsent(decimals, IFlanItem::decimalFormatUpTo);

        return fmt.format(d);
    }

    static String formatDouble(double d)
    {
        return formatDouble(d, 2);
    }

    /**
     * Color a health value by its ratio to max health: green above 66%, yellow above 33%, red below.
     */
    static ChatFormatting healthColor(float health, float maxHealth)
    {
        if (maxHealth <= 0F)
            return ChatFormatting.GREEN;

        float ratio = health / maxHealth;
        if (ratio > 2F / 3F)
            return ChatFormatting.GREEN;
        if (ratio > 1F / 3F)
            return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    /**
     * "[Health] current/max" style tooltip line, colored by the current/max ratio.
     */
    static MutableComponent healthLine(String translationKey, float health, float maxHealth)
    {
        return Component.translatable(translationKey, formatFloat(health, 1), formatFloat(maxHealth, 1))
            .withStyle(healthColor(health, maxHealth));
    }

    static UUID getOrCreateStackUUID(ItemStack stack, String key)
    {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(key))
            tag.putUUID(key, UUID.randomUUID());
        return tag.getUUID(key);
    }
}
