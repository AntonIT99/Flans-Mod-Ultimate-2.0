package com.wolffsarmormod.common.types;

import com.wolffsarmormod.common.guns.EnumFireMode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class GunType extends InfoType
{
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
}
