package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModelMGMuzzleTest
{
    private static ModelRendererTurbo box(ModelMG model, float x, float y, float z,
                                          int width, int height, int depth, float pivotY)
    {
        ModelRendererTurbo part = new ModelRendererTurbo(model, 0, 0, 64, 64);
        part.addBox(x, y, z, width, height, depth);
        part.setRotationPoint(0F, pivotY, 0F);
        return part;
    }

    @Test
    void normalizedBarrelFaceTracksTheRenderedPitch()
    {
        ModelMG model = new ModelMG();
        model.bipodModel = new ModelRendererTurbo[0];
        model.ammoModel = new ModelRendererTurbo[0];
        model.gunModel = new ModelRendererTurbo[] {
            box(model, -4F, -3F, -8F, 8, 6, 10, -6F),
            box(model, -1F, -1F, 2F, 2, 2, 18, -6F)
        };
        model.flipAll();
        for (ModelRendererTurbo part : model.gunModel)
            part.rotateAngleX = -0.5F; // The model may already have rendered before debug mode is enabled.

        Vec3 level = model.getModelMuzzle(0F);
        Vec3 pitched = model.getModelMuzzle(30F);

        assertNotNull(level);
        assertNotNull(pitched);
        assertEquals(0D, level.x, 1.0E-4D);
        assertEquals(6D, level.y, 1.0E-4D);
        assertEquals(-20D, level.z, 1.0E-4D);
        assertEquals(-0.5F, model.gunModel[1].rotateAngleX, 1.0E-4F);
        assertEquals(-4D, pitched.y, 1.0E-4D);
        assertEquals(-20D * Math.cos(Math.toRadians(30D)), pitched.z, 1.0E-4D);
    }

    @Test
    void unmirroredLegacyModelUsesNegativeZAsForward()
    {
        ModelMG model = new ModelMG();
        model.gunModel = new ModelRendererTurbo[] {
            box(model, -2F, -2F, -4F, 4, 4, 12, 6F),
            box(model, -1F, -1F, -14F, 2, 2, 12, 6F),
            box(model, -1F, -2F, 12F, 2, 4, 2, 6F)
        };

        Vec3 muzzle = model.getModelMuzzle(0F);

        assertNotNull(muzzle);
        assertEquals(0D, muzzle.x, 1.0E-4D);
        assertEquals(6D, muzzle.y, 1.0E-4D);
        assertEquals(-14D, muzzle.z, 1.0E-4D);
    }

    @Test
    void flipAllNormalizesAnOldPositiveZBarrelToLegacyForward()
    {
        ModelMG model = new ModelMG();
        model.gunModel = new ModelRendererTurbo[] {
            box(model, -1F, -1F, -2F, 2, 2, 12, -6F),
            box(model, -1F, -1F, 10F, 2, 2, 5, -6F),
            box(model, -1F, -1F, 15F, 2, 2, 5, -6F)
        };
        model.flipAll();

        Vec3 muzzle = model.getModelMuzzle(0F);

        assertNotNull(muzzle);
        assertEquals(-20D, muzzle.z, 1.0E-4D);
    }
}
