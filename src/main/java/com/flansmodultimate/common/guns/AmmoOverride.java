package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.BulletType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Per-weapon replacement for the statistics an ammunition item normally supplies.
 *
 * <p>Generic ammunition is deliberately shared: one {@code 75mm AP Tank Shell} item is
 * loaded by a dozen different vehicles, and one belt is fed to several guns. The
 * ammunition therefore cannot describe every weapon that fires it. These overrides
 * let the firing weapon state what that shared item does <em>out of this barrel</em>
 * without splitting the content pack's item into a dozen near-duplicates.
 *
 * <p>An override is resolved strictly per ammunition item: declaring a mass for one
 * short name says nothing about any other ammunition the same weapon accepts.
 *
 * <p>Field precedence, highest first:
 * <ol>
 *   <li>a scalar override declared here;</li>
 *   <li>the matching field of a round supplied by {@code AddRoundForAmmo};</li>
 *   <li>the matching field of a round in the ammunition's own {@code AddRound} belt;</li>
 *   <li>the ammunition's own top-level value.</li>
 * </ol>
 *
 * <p>{@code AddRoundForAmmo} replaces the ammunition's belt outright for this weapon
 * rather than appending to it, so a weapon can feed a different mix through a shared
 * belt item.
 *
 * @param massGrams               projectile mass in grams, or null to keep the ammunition's
 * @param bulletSpeedBlocksPerTick muzzle velocity already divided by 20, or null to keep the ammunition's
 * @param explosiveMassKg         bursting charge in kg TNT equivalent, or null to keep the ammunition's
 * @param penetrationAt100mMm     penetration in millimetres at 100 m, or null to keep the ammunition's
 * @param rounds                  replacement belt, empty when {@code AddRoundForAmmo} was not used
 */
public record AmmoOverride(@Nullable Float massGrams,
                           @Nullable Float bulletSpeedBlocksPerTick,
                           @Nullable Float explosiveMassKg,
                           @Nullable Float penetrationAt100mMm,
                           @Unmodifiable List<BulletType.RoundEntry> rounds)
{
    public static final AmmoOverride EMPTY = new AmmoOverride(null, null, null, null, List.of());

    public AmmoOverride
    {
        rounds = rounds == null ? List.of() : List.copyOf(rounds);
    }

    /** Whether this override replaces the ammunition's own {@code AddRound} belt. */
    public boolean hasRounds()
    {
        return !rounds.isEmpty();
    }

    public boolean isEmpty()
    {
        return massGrams == null && bulletSpeedBlocksPerTick == null && explosiveMassKg == null
            && penetrationAt100mMm == null && rounds.isEmpty();
    }

    /** Total length of the repeating belt, or zero when no belt is declared. */
    public int periodLength()
    {
        int total = 0;
        for (BulletType.RoundEntry entry : rounds)
            total += entry.count();
        return total;
    }

    /**
     * The replacement round occupying the given position in the magazine.
     *
     * @param shotsFired position in the magazine
     * @return the round's statistics, or null when this override declares no belt
     */
    @Nullable
    public BulletType.RoundStats statsForShot(int shotsFired)
    {
        int period = periodLength();
        if (period <= 0)
            return null;
        int k = Math.floorMod(shotsFired, period);
        for (BulletType.RoundEntry entry : rounds)
        {
            if (k < entry.count())
                return entry.stats();
            k -= entry.count();
        }
        return rounds.get(rounds.size() - 1).stats();
    }

    // ------------------------------------------------------------- resolution

    public float resolveMass(BulletType bulletType, int shotsFired)
    {
        if (massGrams != null)
            return massGrams;
        BulletType.RoundStats stats = statsForShot(shotsFired);
        if (stats != null)
            return stats.mass();
        return bulletType.getMass(shotsFired);
    }

    /**
     * @param weaponBulletSpeedBlocksPerTick velocity the firing weapon supplies, used only when
     *                                       neither this override nor the ammunition declares one
     */
    public float resolveBulletSpeed(BulletType bulletType, int shotsFired, float weaponBulletSpeedBlocksPerTick)
    {
        return resolveBulletSpeed(bulletType, shotsFired, weaponBulletSpeedBlocksPerTick, true);
    }

    /**
     * @param weaponBulletSpeedBlocksPerTick velocity the firing weapon supplies, used only when
     *                                       neither this override nor the ammunition declares one
     * @param useDefaultFallback             when false, zero is returned if nothing declares a velocity,
     *                                       which marks the shot as an instant raytrace
     */
    public float resolveBulletSpeed(BulletType bulletType, int shotsFired, float weaponBulletSpeedBlocksPerTick,
                                    boolean useDefaultFallback)
    {
        if (bulletSpeedBlocksPerTick != null)
            return bulletType.applySpeedMultiplier(bulletSpeedBlocksPerTick);
        BulletType.RoundStats stats = statsForShot(shotsFired);
        if (stats != null && stats.bulletSpeed() > 0F)
            return bulletType.applySpeedMultiplier(stats.bulletSpeed());
        return bulletType.getBulletSpeed(shotsFired, weaponBulletSpeedBlocksPerTick, useDefaultFallback);
    }

    public float resolveExplosiveMass(BulletType bulletType, int shotsFired)
    {
        if (explosiveMassKg != null)
            return explosiveMassKg;
        BulletType.RoundStats stats = statsForShot(shotsFired);
        if (stats != null)
            return stats.explosiveMass();
        return bulletType.hasDifferentRounds()
            ? bulletType.statsForShot(shotsFired).explosiveMass()
            : bulletType.getExplosiveMass();
    }

    public float resolvePenetrationAt100m(BulletType bulletType, int shotsFired)
    {
        if (penetrationAt100mMm != null)
            return penetrationAt100mMm;
        BulletType.RoundStats stats = statsForShot(shotsFired);
        if (stats != null)
            return stats.penetrationAt100m();
        return bulletType.getPenetrationAt100m(shotsFired);
    }
}
