package com.flansmodultimate.client.distant;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistantImpostorPassTest
{
    private static final float[] TRANSLATION = {1F, 0F, 0F, 5F, 0F, 1F, 0F, 6F, 0F, 0F, 1F, 7F, 0F, 0F, 0F, 1F};
    private static final float[] SCALE = {2F, 0F, 0F, 0F, 0F, 2F, 0F, 0F, 0F, 0F, 2F, 0F, 0F, 0F, 0F, 1F};

    @Test
    void rowMajorMatricesKeepTheirTranslationColumn()
    {
        Vector4f moved = DistantImpostorPass.rowMajor(TRANSLATION).transform(new Vector4f(0F, 0F, 0F, 1F));
        assertVector(new Vector4f(5F, 6F, 7F, 1F), moved);
    }

    @Test
    void rowMajorProductAppliesTheRightMatrixFirst()
    {
        // The projection applies after the view, as it does in Distant Horizons
        Vector4f point = DistantImpostorPass.rowMajorProduct(TRANSLATION, SCALE).transform(new Vector4f(1F, 1F, 1F, 1F));
        assertVector(new Vector4f(7F, 8F, 9F, 1F), point);
    }

    @Test
    void rowMajorMatchesRowTimesColumnForAnyMatrix()
    {
        float[] values = new float[16];
        for (int i = 0; i < 16; i++)
            values[i] = i * 0.37F - 2F + (i % 5) * 0.11F;
        float[] vector = {0.3F, -1.7F, 2.2F, 1F};
        Matrix4f matrix = DistantImpostorPass.rowMajor(values);
        Vector4f actual = matrix.transform(new Vector4f(vector[0], vector[1], vector[2], vector[3]));
        float[] expected = new float[4];
        for (int row = 0; row < 4; row++)
            for (int column = 0; column < 4; column++)
                expected[row] += values[row * 4 + column] * vector[column];
        assertVector(new Vector4f(expected[0], expected[1], expected[2], expected[3]), actual);
    }

    private static void assertVector(Vector4f expected, Vector4f actual)
    {
        assertEquals(expected.x(), actual.x(), 1E-5F);
        assertEquals(expected.y(), actual.y(), 1E-5F);
        assertEquals(expected.z(), actual.z(), 1E-5F);
        assertEquals(expected.w(), actual.w(), 1E-5F);
    }
}
