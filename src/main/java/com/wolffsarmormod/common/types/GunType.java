package com.wolffsarmormod.common.types;

import com.wolffsarmormod.common.guns.EnumFireMode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class GunType extends PaintableType implements IScope
{
    /**
     * The list of bullet types that can be used in this gun
     */
    public List<ShootableType> ammo = new ArrayList<>(), nonExplosiveAmmo = new ArrayList<>();
    /**
     * Whether the player can press the reload key (default R) to reload this gun
     */
    public boolean canForceReload = true;
    /**
     * The time (in ticks) it takes to reload this gun
     */
    public int reloadTime;
    /**
     * The amount to recoil the player's view by when firing a single shot from this gun
     */
    @Getter
    protected int recoil;
    /**
     * The firing mode of the gun. One of semi-auto, full-auto, minigun or burst
     */
    @Getter
    protected EnumFireMode mode = EnumFireMode.FULLAUTO;

    protected boolean scopeOverlay = false;

    /**
     * Whether the default scope has an overlay
     */
    public boolean hasScopeOverlay() {
        return scopeOverlay;
    }

    @Override
    public float getFOVFactor()
    {
        return 0;
    }

    @Override
    public float getZoomFactor()
    {
        return 0;
    }

    @Override
    public boolean hasZoomOverlay()
    {
        return false;
    }

    @Override
    public String getZoomOverlay()
    {
        return "";
    }
}
