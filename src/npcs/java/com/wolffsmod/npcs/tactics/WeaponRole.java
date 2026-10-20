package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.FlansEquipment;
import com.flansmodultimate.api.WeaponTraits;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

/**
 * What a ranged weapon is for, as far as where its holder should stand: the share of its firing range it fights at when
 * its tactic distance is automatic, and the tactic a soldier class carrying it starts with.
 */
public enum WeaponRole
{
    /** Shotguns: several pellets a shot. */
    CLOSE(0.3D, Tactic.ADVANCE),
    /** Rifles, carbines, sub-machine guns, pistols and bows. */
    GENERAL(0.6D, Tactic.COVER),
    /** Guns with a magnifying sight. */
    MARKSMAN(0.85D, Tactic.STAND_OFF),
    /** Automatic guns fed from a large magazine or belt. */
    SUPPORT(0.7D, Tactic.HOLD),
    /** Single-shot explosive launchers. */
    LAUNCHER(0.7D, Tactic.COVER),
    /** Grenades and other thrown weapons. */
    THROWN(0.4D, Tactic.ADVANCE);

    /** Sight magnification from which a gun counts as a marksman's. */
    static final float MARKSMAN_ZOOM = 3F;
    /** Rounds an automatic gun holds from which it counts as a support weapon. */
    static final int SUPPORT_MAGAZINE = 50;

    private final double share;
    private final Tactic tactic;

    WeaponRole(double share, Tactic tactic)
    {
        this.share = share;
        this.tactic = tactic;
    }

    /** @return the share of its firing range the NPC fights at when its tactic distance is left automatic */
    public double share()
    {
        return share;
    }

    /** @return the tactic a soldier class with this weapon starts with */
    public Tactic tactic()
    {
        return tactic;
    }

    /**
     * @param ammunition
     *            what the weapon is loaded from, which sets the magazine an empty gun is measured with; may be empty
     */
    public static WeaponRole of(ItemStack weapon, ItemStack ammunition)
    {
        return FlansEquipment.getWeaponProperties(weapon, ammunition).map(properties -> of(properties, FlansEquipment.getWeaponTraits(weapon, ammunition).orElse(null))).orElse(GENERAL);
    }

    static WeaponRole of(EquippedWeaponProperties properties, @Nullable WeaponTraits traits)
    {
        if (properties.throwable())
            return THROWN;
        if (properties.projectiles() > 1)
            return CLOSE;
        if (properties.impact().explosionRadius() > 0D && !properties.automaticFire())
            return LAUNCHER;
        if (traits != null && traits.scopeZoom() >= MARKSMAN_ZOOM)
            return MARKSMAN;
        if (properties.automaticFire() && traits != null && traits.magazineCapacity() >= SUPPORT_MAGAZINE)
            return SUPPORT;
        return GENERAL;
    }
}
