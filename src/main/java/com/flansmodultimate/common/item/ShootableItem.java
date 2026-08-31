package com.flansmodultimate.common.item;

import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModClientConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public abstract class ShootableItem extends Item
{
    protected final String shortname;
    private static final String NBT_ROUNDS = "rounds";

    protected ShootableItem(ShootableType configType)
    {
        super(createProperties(configType));
        shortname = configType.getShortName();
    }

    public abstract ShootableType getConfigType();

    public static boolean hasRoundsLeft(ItemStack stack)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ShootableItem))
            return false;
        return getRoundsRemaining(stack) > 0;
    }

    public static int getRoundsRemaining(ItemStack stack)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ShootableItem item))
            return 0;

        ShootableType type = item.getConfigType();
        int roundsPerItem = type.getRoundsPerItem();

        if (roundsPerItem <= 1)
        {
            return stack.getCount();
        }

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_ROUNDS))
        {
            return tag.getInt(NBT_ROUNDS);
        }
        return roundsPerItem;
    }

    public static void setRoundsRemaining(ItemStack stack, int rounds)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ShootableItem item))
            return;

        ShootableType type = item.getConfigType();
        int roundsPerItem = type.getRoundsPerItem();

        if (roundsPerItem <= 1)
        {
            stack.setCount(Math.max(0, rounds));
            return;
        }

        stack.getOrCreateTag().putInt(NBT_ROUNDS, Math.max(0, Math.min(rounds, roundsPerItem)));
    }

    public static int getMaxRounds(ItemStack stack)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ShootableItem item))
            return 0;
        return item.getConfigType().getRoundsPerItem();
    }

    public static int getTotalRounds(ItemStack stack)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ShootableItem item))
            return 0;

        ShootableType type = item.getConfigType();
        int roundsPerItem = type.getRoundsPerItem();

        if (roundsPerItem <= 1)
        {
            return stack.getCount();
        }

        int currentRounds = getRoundsRemaining(stack);
        int stackCount = stack.getCount();
        return (stackCount - 1) * roundsPerItem + currentRounds;
    }

    public static boolean consumeRound(ItemStack stack)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof ShootableItem item))
            return false;

        ShootableType type = item.getConfigType();
        int roundsPerItem = type.getRoundsPerItem();

        if (roundsPerItem <= 1)
        {
            if (stack.getCount() > 0)
            {
                stack.shrink(1);
                return true;
            }
            return false;
        }

        int currentRounds = getRoundsRemaining(stack);
        if (currentRounds > 0)
        {
            setRoundsRemaining(stack, currentRounds - 1);
            if (currentRounds - 1 <= 0 && stack.getCount() > 1)
            {
                stack.shrink(1);
                setRoundsRemaining(stack, roundsPerItem);
            }
            return true;
        }
        else if (stack.getCount() > 1)
        {
            stack.shrink(1);
            setRoundsRemaining(stack, roundsPerItem - 1);
            return true;
        }
        return false;
    }

    private static Properties createProperties(ShootableType configType)
    {
        Properties p = new Properties();
        int maxStack = Math.max(1, configType.getMaxStackSize());
        p.stacksTo(maxStack);
        return p;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack)
    {
        ModClientConfig config = ModClientConfig.get();
        if (config != null && !config.showShootableDurabilityBars)
            return false;

        int maxRounds = getConfigType().getRoundsPerItem();
        return maxRounds > 1 && getRoundsRemaining(stack) < maxRounds;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack)
    {
        int maxRounds = getConfigType().getRoundsPerItem();
        if (maxRounds <= 1)
            return 13;

        int rounds = Mth.clamp(getRoundsRemaining(stack), 0, maxRounds);
        return Math.round(13F * rounds / maxRounds);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack)
    {
        int maxRounds = getConfigType().getRoundsPerItem();
        if (maxRounds <= 1)
            return 0x00FF00;

        float fill = Mth.clamp((float)getRoundsRemaining(stack) / maxRounds, 0F, 1F);
        return Mth.hsvToRgb(fill / 3F, 1F, 1F);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        ShootableType configType = getConfigType();

        if (configType.getRoundsPerItem() > 1)
        {
            int currentRounds = getRoundsRemaining(stack);
            int maxRounds = configType.getRoundsPerItem();
            int stackCount = stack.getCount();
            if (stackCount > 1)
            {
                int totalRounds = getTotalRounds(stack);
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.ROUNDS),
                    Component.translatable(TooltipKeys.ROUNDS_TOTAL, currentRounds, maxRounds, stackCount, totalRounds)));
            }
            else
            {
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.ROUNDS), currentRounds + "/" + maxRounds));
            }
        }
        else if (configType.getRoundsPerItem() == 1)
        {
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.ROUNDS), String.valueOf(stack.getCount())));
        }

        if (configType.getNumBullets() > 1)
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.SHOT), String.valueOf(configType.getNumBullets())));

        if (configType.useKineticDamageSystem())
        {
            if (configType instanceof BulletType bulletType)
            {
                boolean hasDifferentRounds = bulletType.hasDifferentRounds();
                boolean hasRoundKineticDamage = hasDifferentRounds && bulletType.getPeriod().stream()
                    .anyMatch(round -> round.stats().mass() > 0F && round.stats().bulletSpeed() > 0F);

                if (hasDifferentRounds)
                {
                    tooltipComponents.add(Component.translatable(TooltipKeys.MASS).append(":").withStyle(ChatFormatting.BLUE));
                    bulletType.getPeriod().forEach(round ->
                        tooltipComponents.add(Component.literal("  " + round.name() + " " + IFlanItem.formatFloat(round.stats().mass()) + " g").withStyle(ChatFormatting.GRAY)));
                }
                else
                {
                    tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.MASS), IFlanItem.formatFloat(configType.getMass()) + " g"));
                }

                if (hasRoundKineticDamage)
                {
                    tooltipComponents.add(Component.translatable(TooltipKeys.DAMAGE).append(":").withStyle(ChatFormatting.BLUE));
                    bulletType.getPeriod().forEach(round ->
                    {
                        if (round.stats().mass() > 0F && round.stats().bulletSpeed() > 0F)
                            tooltipComponents.add(Component.literal("  " + round.name() + " " + IFlanItem.formatFloat(ShootingHelper.getKineticDamage(round.stats().mass(), round.stats().bulletSpeed()), 1)).withStyle(ChatFormatting.GRAY));
                    });
                }
                else if (bulletType.getBulletSpeed() > 0F)
                {
                    tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.DAMAGE), IFlanItem.formatFloat(ShootingHelper.getKineticDamage(configType.getMass(), bulletType.getBulletSpeed()), 1)));
                }

            }
            else
            {
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.MASS), IFlanItem.formatFloat(configType.getMass()) + " g"));
            }
        }
        else
            IFlanItem.appendDamageStats(tooltipComponents, configType.getDamage(), TooltipKeys.DAMAGE);

        if (configType.useNewExplosionSystem())
        {
            if (configType instanceof BulletType bulletType && bulletType.hasDifferentRounds())
            {
                tooltipComponents.add(Component.translatable(TooltipKeys.EXPLOSIVE_MASS_TNT).append(":").withStyle(ChatFormatting.BLUE));
                bulletType.getPeriod().forEach(round ->
                    tooltipComponents.add(Component.literal("  " + round.name() + " " + IFlanItem.formatFloat(round.stats().explosiveMass(), 3) + " kg").withStyle(ChatFormatting.GRAY)));
            }
            else
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSIVE_MASS_TNT), IFlanItem.formatFloat(configType.getExplosiveMass(), 3) + " kg"));
        }

        if (configType.getExplosionRadius() > 0F)
        {
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_RADIUS), IFlanItem.formatFloat(configType.getExplosionRadius(), 1)));
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_POWER), IFlanItem.formatFloat(configType.getExplosionPower(), 1)));
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_BLAST_RADIUS), IFlanItem.formatFloat(configType.getBlastRadius(), 1)));
            IFlanItem.appendDamageStats(tooltipComponents, configType.getExplosionBlastDamage(), TooltipKeys.EXPLOSION_BLAST_DAMAGE);

            if (configType.getFragRadius() > 0F)
            {
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_FRAG_RADIUS), IFlanItem.formatFloat(configType.getFragRadius(), 1)));
                IFlanItem.appendDamageStats(tooltipComponents, configType.getExplosionFragDamage(), TooltipKeys.EXPLOSION_FRAG_DAMAGE);
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_FRAG_INTENSITY), IFlanItem.formatFloat(configType.getFragIntensity(), 1)));
            }
        }

        if (configType.getFireRadius() > 0F)
        {
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.FIRE_RADIUS), IFlanItem.formatFloat(configType.getFireRadius(), 1)));
        }

        if (configType.getFallSpeed() > 1F || configType.getFallSpeed() < 1F)
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.GRAVITY_FACTOR), IFlanItem.formatFloat(configType.getFallSpeed())));

        if (configType.getBulletSpread() > 0F)
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.DISPERSION), IFlanItem.formatFloat(configType.getDispersionForDisplay()) + "°"));
    }
}
