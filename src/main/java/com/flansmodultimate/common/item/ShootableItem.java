package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.ShootableType;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public abstract class ShootableItem extends Item
{
    protected final String shortname;
    private static final String TAG_ROUNDS = "Rounds";

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
        if (tag != null && tag.contains(TAG_ROUNDS))
        {
            return tag.getInt(TAG_ROUNDS);
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

        stack.getOrCreateTag().putInt(TAG_ROUNDS, Math.max(0, Math.min(rounds, roundsPerItem)));
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        if (getConfigType().getRoundsPerItem() > 1)
        {
            int currentRounds = getRoundsRemaining(stack);
            int maxRounds = getConfigType().getRoundsPerItem();
            int stackCount = stack.getCount();
            if (stackCount > 1)
            {
                int totalRounds = getTotalRounds(stack);
                tooltipComponents.add(IFlanItem.statLine("Rounds", currentRounds + "/" + maxRounds + " (x" + stackCount + " = " + totalRounds + " total)"));
            }
            else
            {
                tooltipComponents.add(IFlanItem.statLine("Rounds", currentRounds + "/" + maxRounds));
            }
        }
        else if (getConfigType().getRoundsPerItem() == 1)
        {
            tooltipComponents.add(IFlanItem.statLine("Rounds", String.valueOf(stack.getCount())));
        }

        if (getConfigType().getNumBullets() > 1)
            tooltipComponents.add(IFlanItem.statLine("Shot", String.valueOf(getConfigType().getNumBullets())));

        if (getConfigType().useKineticDamageSystem())
        {
            if (getConfigType() instanceof BulletType bulletType && bulletType.hasDifferentRounds())
            {
                tooltipComponents.add(Component.literal("Mass:").withStyle(ChatFormatting.BLUE));
                bulletType.getPeriod().forEach(round ->
                    tooltipComponents.add(Component.literal("  " + round.name() + " " + IFlanItem.formatFloat(round.stats().mass()) + "g").withStyle(ChatFormatting.GRAY)));
            }
            else
                tooltipComponents.add(IFlanItem.statLine("Mass", IFlanItem.formatFloat(getConfigType().getMass()) + "g"));
        }
        else
            IFlanItem.appendDamageStats(tooltipComponents, getConfigType().getDamage(), "Damage");

        if (getConfigType().useNewExplosionSystem())
        {
            if (getConfigType() instanceof BulletType bulletType && bulletType.hasDifferentRounds())
            {
                tooltipComponents.add(Component.literal("Explosive Mass (TNT):").withStyle(ChatFormatting.BLUE));
                bulletType.getPeriod().forEach(round ->
                    tooltipComponents.add(Component.literal("  " + round.name() + " " + IFlanItem.formatFloat(round.stats().explosiveMass(), 3) + "kg").withStyle(ChatFormatting.GRAY)));
            }
            else
                tooltipComponents.add(IFlanItem.statLine("Explosive Mass (TNT)", IFlanItem.formatFloat(getConfigType().getExplosiveMass(), 3) + "kg"));
        }

        if (getConfigType().getExplosionRadius() > 0F)
        {
            tooltipComponents.add(IFlanItem.statLine("Explosion Radius", IFlanItem.formatFloat(getConfigType().getExplosionRadius(), 1)));
            tooltipComponents.add(IFlanItem.statLine("Explosion Power", IFlanItem.formatFloat(getConfigType().getExplosionPower(), 1)));
            tooltipComponents.add(IFlanItem.statLine("Explosion Blast Radius", IFlanItem.formatFloat(getConfigType().getBlastRadius(), 1)));
            IFlanItem.appendDamageStats(tooltipComponents, getConfigType().getExplosionBlastDamage(), "Explosion Blast Damage");

            if (getConfigType().getFragRadius() > 0F)
            {
                tooltipComponents.add(IFlanItem.statLine("Explosion Frag Radius", IFlanItem.formatFloat(getConfigType().getFragRadius(), 1)));
                IFlanItem.appendDamageStats(tooltipComponents, getConfigType().getExplosionFragDamage(), "Explosion Frag Damage");
                tooltipComponents.add(IFlanItem.statLine("Explosion Frag Intensity", IFlanItem.formatFloat(getConfigType().getFragIntensity(), 1)));
            }
        }

        if (getConfigType().getFireRadius() > 0F)
        {
            tooltipComponents.add(IFlanItem.statLine("Fire Radius", IFlanItem.formatFloat(getConfigType().getFireRadius(), 1)));
        }

        if (getConfigType().getFallSpeed() > 1F || getConfigType().getFallSpeed() < 1F)
            tooltipComponents.add(IFlanItem.statLine("Gravity Factor", IFlanItem.formatFloat(getConfigType().getFallSpeed())));

        if (getConfigType().getBulletSpread() > 0F)
            tooltipComponents.add(IFlanItem.statLine("Dispersion", IFlanItem.formatFloat(getConfigType().getDispersionForDisplay()) + "°"));
    }
}
