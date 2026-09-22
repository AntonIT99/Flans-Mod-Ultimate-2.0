package com.flansmodultimate.common.item;

import com.flansmodultimate.common.explosions.FlanExplosion;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

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

    /**
     * Position in the magazine of the round about to be fired, counting from the first one loaded.
     *
     * <p>This is what selects the round of an {@code AddRound} belt, so it has to count up as the item
     * empties. Items holding a single round have no belt position and always report zero.
     */
    public static int getRoundsFired(ItemStack stack)
    {
        int roundsPerItem = getMaxRounds(stack);

        if (roundsPerItem <= 1)
            return 0;

        return Math.max(0, roundsPerItem - getRoundsRemaining(stack));
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

        appendAmmoStats(tooltipComponents, null);
    }

    /**
     * Stack-independent ammunition stats, shared by the item tooltip and the driveable ammo tooltips.
     *
     * @param context the weapon the round is shown for, whose multipliers and per-ammunition overrides are
     *                resolved into the values; null to describe the ammunition on its own
     */
    public void appendAmmoStats(@NotNull List<Component> tooltipComponents, @Nullable AmmoStatContext context)
    {
        ShootableType configType = getConfigType();
        BulletType bulletType = configType instanceof BulletType type ? type : null;
        List<RoundView> rounds = bulletType == null ? List.of() : roundViews(bulletType, context);

        int numBullets = context != null ? context.numBullets() : configType.getNumBullets();
        if (numBullets > 1)
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.SHOT), String.valueOf(numBullets)));

        // What loading this round does to the weapon, read the same way as an attachment's
        // modifiers so the two are directly comparable. With a weapon context, damage and
        // spread are already folded into the resolved values below.
        if (context == null && configType.getDamageMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.DAMAGE), configType.getDamageMultiplier(), false));

        if (context == null && configType.getSpreadMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.BULLET_SPREAD), configType.getSpreadMultiplier(), true));

        if (configType.getRecoilMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.RECOIL), configType.getRecoilMultiplier(), true));

        if (configType.getReloadTimeMultiplier() != 1F)
            tooltipComponents.add(IFlanItem.modifierLine(Component.translatable(TooltipKeys.RELOAD_TIME), configType.getReloadTimeMultiplier(), true));

        if (configType.useKineticDamageSystem())
        {
            if (bulletType != null)
            {
                appendPerRound(tooltipComponents, TooltipKeys.MASS, rounds,
                    shot -> IFlanItem.formatFloat(projectileMass(bulletType, shot, context)) + " g");
                appendPerRound(tooltipComponents, TooltipKeys.DAMAGE, rounds, shot -> {
                    float mass = projectileMass(bulletType, shot, context);
                    float velocity = muzzleVelocity(bulletType, shot, context);
                    return mass > 0F && velocity > 0F
                        ? IFlanItem.formatFloat(ShootingHelper.getKineticDamage(mass, velocity), 1) : null;
                });
            }
            else
            {
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.MASS), IFlanItem.formatFloat(configType.getMass()) + " g"));
            }
        }
        else
        {
            // Legacy damage is scaled by the firing weapon's damage, e.g. a vehicle's DamageMultiplierPrimary.
            float weaponDamage = context != null && bulletType != null ? context.fireable(bulletType).getDamage() : 1F;
            IFlanItem.appendDamageStats(tooltipComponents, configType.getDamage(), TooltipKeys.DAMAGE, weaponDamage);
        }

        if (configType.useNewExplosionSystem())
        {
            if (bulletType != null)
                appendPerRound(tooltipComponents, TooltipKeys.EXPLOSIVE_MASS_TNT, rounds,
                    shot -> IFlanItem.formatMassKg(explosiveMass(bulletType, shot, context)));
            else
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSIVE_MASS_TNT), IFlanItem.formatMassKg(configType.getExplosiveMass())));
        }

        if (configType.getExplosionRadius() > 0F)
        {
            // Pulled from a real Stats object rather than the individual raw getters, so the
            // tooltip shows exactly what FlanExplosion will simulate: the crater cap, and the
            // blast/frag flattening curve for heavy charges, both applied.
            FlanExplosion.Stats stats = context != null && bulletType != null
                ? bulletType.getExplosionStatsForShot(context.shot(bulletType, 0))
                : configType.getExplosionStats(null);

            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_RADIUS), IFlanItem.formatFloat(stats.explosionRadius(), 1)));
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_POWER), IFlanItem.formatFloat(stats.explosionPower(), 1)));
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_BLAST_RADIUS), IFlanItem.formatFloat(stats.blastRadius(), 1)));
            IFlanItem.appendDamageStats(tooltipComponents, stats.blastDamage(), TooltipKeys.EXPLOSION_BLAST_DAMAGE);

            if (stats.fragRadius() > 0F)
            {
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_FRAG_RADIUS), IFlanItem.formatFloat(stats.fragRadius(), 1)));
                IFlanItem.appendDamageStats(tooltipComponents, stats.fragDamage(), TooltipKeys.EXPLOSION_FRAG_DAMAGE);
                tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.EXPLOSION_FRAG_INTENSITY), IFlanItem.formatFloat(stats.fragIntensity(), 1)));
            }
        }

        if (configType.getFireRadius() > 0F)
        {
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.FIRE_RADIUS), IFlanItem.formatFloat(configType.getFireRadius(), 1)));
        }

        if (configType.getFallSpeed() > 1F || configType.getFallSpeed() < 1F)
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.GRAVITY_FACTOR), IFlanItem.formatFloat(configType.getFallSpeed())));

        float dispersion = context != null && bulletType != null
            ? Mth.RAD_TO_DEG * ShootingHelper.ANGULAR_SPREAD_FACTOR * context.shot(bulletType, 0).getSpread()
            : configType.getBulletSpread() > 0F ? configType.getDispersionForDisplay() : 0F;
        if (dispersion > 0F && (context == null || context.showLaunchStats()))
            tooltipComponents.add(IFlanItem.statLine(Component.translatable(TooltipKeys.DISPERSION), IFlanItem.formatFloat(dispersion) + "°"));
    }

    /** A round of the belt a weapon feeds, with the magazine position its stats are resolved at. */
    protected record RoundView(String name, int count, int shot) {}

    protected static List<RoundView> roundViews(BulletType type, @Nullable AmmoStatContext context)
    {
        List<BulletType.RoundEntry> entries = context != null ? context.rounds(type)
            : type.hasDifferentRounds() ? type.getPeriod() : List.of();
        List<RoundView> views = new ArrayList<>(entries.size());
        int shot = 0;
        for (BulletType.RoundEntry entry : entries)
        {
            views.add(new RoundView(entry.name(), entry.count(), shot));
            shot += entry.count();
        }
        return views;
    }

    /** One stat line, or a labelled list with one line per belt round; null values are skipped. */
    protected static void appendPerRound(List<Component> tooltip, String labelKey, List<RoundView> rounds, IntFunction<String> valueAtShot)
    {
        if (rounds.isEmpty())
        {
            String value = valueAtShot.apply(0);
            if (value != null)
                tooltip.add(IFlanItem.statLine(Component.translatable(labelKey), value));
            return;
        }

        List<Component> lines = new ArrayList<>();
        for (RoundView round : rounds)
        {
            String value = valueAtShot.apply(round.shot());
            if (value != null)
                lines.add(Component.literal("  " + round.name() + " " + value).withStyle(ChatFormatting.GRAY));
        }
        if (lines.isEmpty())
            return;
        tooltip.add(Component.translatable(labelKey).append(":").withStyle(ChatFormatting.BLUE));
        tooltip.addAll(lines);
    }

    protected static float projectileMass(BulletType type, int shot, @Nullable AmmoStatContext context)
    {
        return context != null ? context.shot(type, shot).getProjectileMass() : type.getMass(shot);
    }

    /** Muzzle velocity in blocks per tick; without a weapon, zero when the ammunition declares none. */
    protected static float muzzleVelocity(BulletType type, int shot, @Nullable AmmoStatContext context)
    {
        return context != null ? context.shot(type, shot).getMuzzleVelocity() : type.getBulletSpeed(shot, 0F, false);
    }

    protected static float explosiveMass(BulletType type, int shot, @Nullable AmmoStatContext context)
    {
        if (context != null)
            return context.shot(type, shot).getExplosiveMass();
        return type.hasDifferentRounds() ? type.statsForShot(shot).explosiveMass() : type.getExplosiveMass();
    }

    protected static float penetrationAt100m(BulletType type, int shot, @Nullable AmmoStatContext context)
    {
        return context != null ? context.shot(type, shot).getPenetrationAt100m() : type.getPenetrationAt100m(shot);
    }

    protected static float penetratingPower(BulletType type, int shot, @Nullable AmmoStatContext context)
    {
        return context != null ? context.shot(type, shot).getPenetratingPower() : type.getPenetratingPower();
    }
}
