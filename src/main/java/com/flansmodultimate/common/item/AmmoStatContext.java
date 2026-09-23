package com.flansmodultimate.common.item;

import com.flansmodultimate.common.guns.AmmoOverride;
import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.ShootableType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.function.Supplier;

/**
 * The weapon a round is described for, so a tooltip shows the values that weapon actually fires with:
 * its damage and spread, the velocity it supplies, and the per-ammunition overrides it or its platform declare.
 *
 * @param weapon          builds a fresh weapon side of the shot; called per lookup because
 *                        {@link FireableGun#applyAmmunition} mutates the instance
 * @param platform        the entity carrying the weapon, whose own per-ammunition overrides are the fallback
 * @param numBullets      projectiles per shot out of this weapon
 * @param fixedShot       magazine position every stat is resolved at, describing a single round of a mix;
 *                        negative to describe the whole belt
 * @param showLaunchStats whether muzzle velocity and dispersion are meaningful, which they are not for dropped ordnance
 */
public record AmmoStatContext(Supplier<FireableGun> weapon, @Nullable Entity platform, int numBullets,
                              int fixedShot, boolean showLaunchStats)
{
    public AmmoStatContext(Supplier<FireableGun> weapon, @Nullable Entity platform, int numBullets)
    {
        this(weapon, platform, numBullets, -1, true);
    }

    /** Describes only the round at this magazine position, as if it were its own ammunition. */
    public AmmoStatContext withFixedShot(int shot)
    {
        return new AmmoStatContext(weapon, platform, numBullets, Math.max(0, shot), showLaunchStats);
    }

    public AmmoStatContext withLaunchStats(boolean show)
    {
        return new AmmoStatContext(weapon, platform, numBullets, fixedShot, show);
    }

    public FireableGun fireable(ShootableType ammunition)
    {
        FireableGun gun = weapon.get();
        gun.applyAmmunition(ammunition);
        return gun;
    }

    public FiredShot shot(BulletType ammunition, int shot)
    {
        return new FiredShot(fireable(ammunition), ammunition, platform, null, fixedShot >= 0 ? fixedShot : shot);
    }

    /**
     * The belt this weapon feeds: its {@code AddRoundForAmmo} replacement, else the ammunition's own mix.
     * Empty when a single round is being described.
     */
    public List<BulletType.RoundEntry> rounds(BulletType ammunition)
    {
        if (fixedShot >= 0)
            return List.of();
        AmmoOverride override = shot(ammunition, 0).getAmmoOverride();
        if (override.hasRounds())
            return override.rounds();
        return ammunition.hasDifferentRounds() ? ammunition.getPeriod() : List.of();
    }
}
