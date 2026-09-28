package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AA gun barrels are measured relative to the barrel pivot they are drawn at.
 * Most legacy AA gun models mirror themselves at the end of their constructor,
 * and the measurement has to follow that mirror.
 */
class ModelAAGunMuzzleTest
{
    private static class LegacyThreeBarrelModel extends ModelAAGun
    {
        LegacyThreeBarrelModel()
        {
            for (int i = 0; i < 3; i++)
                barrelModel[i] = new ModelRendererTurbo[] { box(this, 0F, 0F, i * 4F, 20, 2, 2) };
            flipAll();
        }
    }

    private static ModelRendererTurbo box(ModelAAGun model, float x, float y, float z, int width, int height, int depth)
    {
        ModelRendererTurbo part = new ModelRendererTurbo(model, 0, 0, 512, 512);
        part.addBox(x, y, z, width, height, depth);
        return part;
    }

    /** A breech wider than the bore, and a thin tube off to the gun's right, above the pivot. */
    private static ModelAAGun offsetBarrel()
    {
        ModelAAGun model = new ModelAAGun();
        model.barrelModel = new ModelRendererTurbo[][] { {
            box(model, -10F, -6F, 12F, 20, 8, 12),
            box(model, 10F, -3F, 16F, 60, 2, 2)
        } };
        return model;
    }

    @Test
    void theMuzzleIsTheCentreOfTheFrontFaceNotOfTheWholeBarrel()
    {
        Vec3 muzzle = ModelAAGun.findMuzzlePoint(offsetBarrel().barrelModel[0]);

        assertNotNull(muzzle);
        assertEquals(70D, muzzle.x, 1.0E-4D);
        assertEquals(-2D, muzzle.y, 1.0E-4D, "the bore, not halfway down the breech");
        assertEquals(17D, muzzle.z, 1.0E-4D);
    }

    @Test
    void theMuzzleFollowsTheConstructorTimeFlip()
    {
        ModelAAGun model = offsetBarrel();
        model.flipAll();
        Vec3 muzzle = ModelAAGun.findMuzzlePoint(model.barrelModel[0]);

        assertNotNull(muzzle);
        assertEquals(70D, muzzle.x, 1.0E-4D);
        assertEquals(2D, muzzle.y, 1.0E-4D, "Y is mirrored like the drawn faces");
        assertEquals(-17D, muzzle.z, 1.0E-4D, "and so is the side the barrel sits on");
    }

    @Test
    void legacyModelCanFillInheritedBarrelRows()
    {
        ModelAAGun model = new LegacyThreeBarrelModel();

        ModelAAGun.BarrelOriginData origins = model.getModelBarrelOriginData(3);
        assertNotNull(origins);
        assertEquals(3, origins.muzzles().length);
        assertNull(model.barrelModel[3], "unused inherited slots remain empty");
    }
}
