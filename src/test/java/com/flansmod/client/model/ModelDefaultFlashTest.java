package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Packs place the built-in flash by writing the muzzle itself as
 * {@code animMuzzleFlashPoint}, which is only right while every frame stays centred on
 * the model origin and faces along the barrel.
 */
class ModelDefaultFlashTest
{
    @Test
    void everyFrameIsADiscCentredOnTheOrigin()
    {
        ModelDefaultFlash flash = new ModelDefaultFlash();

        assertEquals(3, flash.flashModel.length, "three animation frames");
        for (ModelRendererTurbo[] frame : flash.flashModel)
        {
            double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            assertTrue(frame[0].appendFaceBounds(bounds));

            double centreX = frame[0].rotationPointX + (bounds[0] + bounds[3]) / 2D;
            double centreY = frame[0].rotationPointY + (bounds[1] + bounds[4]) / 2D;
            double centreZ = frame[0].rotationPointZ + (bounds[2] + bounds[5]) / 2D;
            assertEquals(0D, centreX, 1.0E-4D);
            assertEquals(0D, centreY, 1.0E-4D);
            assertEquals(0D, centreZ, 1.0E-4D);
            assertTrue(bounds[3] - bounds[0] < 0.2D, "thin along the barrel axis");
            assertTrue(bounds[4] - bounds[1] > 8D && bounds[5] - bounds[2] > 8D, "wide across it");
        }
    }
}
