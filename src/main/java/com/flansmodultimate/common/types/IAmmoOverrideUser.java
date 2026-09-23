package com.flansmodultimate.common.types;

import com.flansmodultimate.common.guns.AmmoOverrides;

/**
 * Implemented by the content types that can fire ammunition and may therefore restate
 * what a shared ammunition item does out of their own barrel.
 *
 * <p>Resolution is always against the weapon that actually fires the shot. A driveable
 * firing a shell from its own bank uses the driveable's overrides; the same driveable
 * firing through an {@code AddGun} / {@code PilotGun} mount uses that gun's overrides,
 * because it is the gun that declared the ammunition.
 */
public interface IAmmoOverrideUser
{
    /** Per-ammunition statistic overrides declared by this weapon; never null. */
    AmmoOverrides getAmmoOverrides();
}
