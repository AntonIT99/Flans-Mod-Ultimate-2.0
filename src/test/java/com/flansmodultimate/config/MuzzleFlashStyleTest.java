package com.flansmodultimate.config;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.model.ModelGun;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.render.effects.MuzzleFlashRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MuzzleFlashStyleTest
{
    @Test
    void thePreferredStyleWinsAndSingleStylePacksKeepTheirModel()
    {
        assertEquals("classic", EnumMuzzleFlashStyle.FMU_1_7_10.select("classic", "modern"));
        assertEquals("modern", EnumMuzzleFlashStyle.MC_1_12_2.select("classic", "modern"));
        for (EnumMuzzleFlashStyle style : EnumMuzzleFlashStyle.values())
        {
            assertEquals("classic", style.select("classic", null));
            assertEquals("modern", style.select(null, "modern"));
            assertNull(style.select(null, null));
        }
    }

    @Test
    void dualStylePointsStayAtThePhysicalMuzzleWithNonUnitFlashScale()
    {
        ModelGun model = new ModelGun();
        model.setFlashScale(2.5F);
        model.setMuzzleFlashPoint(new Vector3f(0.8F, 0.2F, -0.1F));
        model.setDefaultBarrelFlashPoint(new Vector3f(0.4F, 0F, 0F));
        assertPoint(3F, 0.5F, -0.25F, MuzzleFlashRenderer.muzzlePosition(model, null, true));
        // MuzzleFlashModel-only packs still interpret these as ordinary model blocks.
        assertPoint(1.2F, 0.2F, -0.1F, MuzzleFlashRenderer.muzzlePosition(model, null, false));
        assertPoint(0.8F, 0.2F, -0.1F, model.getMuzzleFlashPoint());
    }

    @Test
    void attachmentOffsetsReplaceTheDefaultBarrelOffsetForBothStyles()
    {
        ModelGun model = new ModelGun();
        model.setFlashScale(2F);
        model.setMuzzleFlashPoint(new Vector3f(1F, 0.5F, 0F));
        model.setDefaultBarrelFlashPoint(new Vector3f(4F, 0F, 0F));
        ModelAttachment barrel = new ModelAttachment()
        {
            @Override
            public Vector3f getMuzzleFlashPoint(Vector3f point, Vector3f attachPoint)
            {
                return Vector3f.add(point, new Vector3f(0.5F, 0F, 0F), null);
            }
        };
        assertPoint(3F, 1F, 0F, MuzzleFlashRenderer.muzzlePosition(model, barrel, true));
        assertPoint(1.5F, 0.5F, 0F, MuzzleFlashRenderer.muzzlePosition(model, barrel, false));
    }

    @Test
    void oldModernPacksKeepTheAttachmentOriginFallbackWhenTheMuzzleIsUnauthored()
    {
        ModelGun model = new ModelGun();
        model.setBarrelAttachPoint(new Vector3f(2F, 0.25F, 0F));
        model.setFlashScale(3F);
        assertPoint(2F, 0.25F, 0F, MuzzleFlashRenderer.muzzlePosition(model, null, false));
        assertPoint(0F, 0F, 0F, MuzzleFlashRenderer.muzzlePosition(model, null, true));
    }

    private static void assertPoint(float x, float y, float z, Vector3f actual)
    {
        assertEquals(x, actual.x, 1.0E-5F);
        assertEquals(y, actual.y, 1.0E-5F);
        assertEquals(z, actual.z, 1.0E-5F);
    }
}
