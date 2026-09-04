package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.BulletType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KineticPenetrationTest
{
    /** Blocks per tick for a muzzle velocity given in metres per second */
    private static float bpt(double metersPerSecond)
    {
        return (float) (metersPerSecond / 20D);
    }

    @Test
    void servicePistolRoundMatchesTheLegacyDefault()
    {
        // 8 g at 360 m/s is the anchor the default reference was chosen for
        assertEquals(BulletType.DEFAULT_PENETRATING_POWER,
            ShootingHelper.getKineticPenetratingPower(8F, bpt(360D)), 0.01F);
    }

    @Test
    void powerGrowsWithCalibreAcrossTheWholeRange()
    {
        float pistol = ShootingHelper.getKineticPenetratingPower(8F, bpt(360D));
        float intermediate = ShootingHelper.getKineticPenetratingPower(7.9F, bpt(715D));
        float fullPower = ShootingHelper.getKineticPenetratingPower(9.5F, bpt(838D));
        float antiMateriel = ShootingHelper.getKineticPenetratingPower(42F, bpt(890D));
        float cannonShell = ShootingHelper.getKineticPenetratingPower(10200F, bpt(773D));

        assertTrue(pistol < intermediate);
        assertTrue(intermediate < fullPower);
        assertTrue(fullPower < antiMateriel);
        assertTrue(antiMateriel < cannonShell);

        // The cube root has to keep a four-order-of-magnitude energy span inside a usable budget
        assertTrue(cannonShell < 20F, "cannon shell should stay within a sane penetration budget");
    }

    @Test
    void powerGrowsWithBothMassAndVelocity()
    {
        float base = ShootingHelper.getKineticPenetratingPower(8F, bpt(400D));
        assertTrue(ShootingHelper.getKineticPenetratingPower(16F, bpt(400D)) > base);
        assertTrue(ShootingHelper.getKineticPenetratingPower(8F, bpt(800D)) > base);
    }

    @Test
    void unusableInputsFallBackToTheLegacyDefaultInsteadOfZeroOrNan()
    {
        assertEquals(BulletType.DEFAULT_PENETRATING_POWER, ShootingHelper.getKineticPenetratingPower(0F, bpt(400D)));
        assertEquals(BulletType.DEFAULT_PENETRATING_POWER, ShootingHelper.getKineticPenetratingPower(-1F, bpt(400D)));
        assertEquals(BulletType.DEFAULT_PENETRATING_POWER, ShootingHelper.getKineticPenetratingPower(Float.NaN, bpt(400D)));
        assertEquals(BulletType.DEFAULT_PENETRATING_POWER, ShootingHelper.getKineticPenetratingPower(8F, 0D));
        assertEquals(BulletType.DEFAULT_PENETRATING_POWER, ShootingHelper.getKineticPenetratingPower(8F, Double.NaN));
    }
}
