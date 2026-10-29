package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.driveables.damage.WreckLoad;
import com.flansmodultimate.common.driveables.weapons.DriveableAmmoLoader;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;

import java.util.ArrayList;
import java.util.List;

/** Conservative ammunition estimate for cosmetic cook-offs, using the same defaults as driveable rearming. */
record DestructionAmmo(float explosionRadius)
{
    /** Burst sizing saturates before the shared particle ceiling, independently of the hull's dimensions. */
    private static final float MAX_MAGAZINE_BURST_SCALE = 2.25F;
    private static final float BURST_SCALE_PER_RADIUS = 0.25F;

    static DestructionAmmo of(DriveableType type)
    {
        List<ShootableType> ammunition = new ArrayList<>();
        for (int slot = 0; slot < type.getNumAmmoSlots(); slot++)
        {
            GunType gun = type.getGunTypeForAmmoSlot(slot);
            if (gun != null)
                gun.getDefaultAmmo().ifPresent(ammunition::add);
        }
        DriveableAmmoLoader.defaultBankAmmo(type, false).ifPresent(ammunition::add);
        DriveableAmmoLoader.defaultBankAmmo(type, true).ifPresent(ammunition::add);
        float radius = 0F;
        for (ShootableType ammo : ammunition)
        {
            float candidate = ammo.getExplosionRadius();
            if (Float.isFinite(candidate) && candidate > radius)
                radius = candidate;
        }
        return new DestructionAmmo(radius);
    }

    /** Non-explosive rounds crackle; an explosive magazine's burst grows with its remaining load, not hull size. */
    float magazineBurstScale(WreckLoad load)
    {
        if (!load.cooksOff() || !Float.isFinite(explosionRadius) || explosionRadius <= 0F)
            return 0F;
        return Math.min(MAX_MAGAZINE_BURST_SCALE, explosionRadius * BURST_SCALE_PER_RADIUS) * (float) Math.sqrt(load.ammunition());
    }
}
