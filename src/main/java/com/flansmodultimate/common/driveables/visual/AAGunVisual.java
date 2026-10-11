package com.flansmodultimate.common.driveables.visual;

/** What an anti-aircraft gun model reads from the gun it draws. */
public interface AAGunVisual
{
    /** @return how far each barrel is pushed back, in model units */
    float[] getBarrelRecoil();

    /** @return whether the barrel's ammunition part is drawn */
    boolean hasAmmo(int barrelIndex);
}
