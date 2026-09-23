package com.flansmod.client.tmt;

import com.flansmodultimate.client.render.gpu.GeometryRevision;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SameParameterValue", "java:S1104"})
public class TexturedPolygon
{
    public PositionTextureVertex[] vertexPositions;
    public int nVertices;

    private boolean invertNormal;
    private float[] normals;
    private List<Vec3> iNormals;
    private boolean hasTransformVertices;
    private boolean externalNormals;
    private PositionTextureVertex[] observedVertices;
    private Vec3[] observedPositions;
    private float[] observedUvs;
    private int observedCount;
    private long geometryRevision;
    private int[] renderVertexIndices;
    private int[] legacyRenderVertexIndices;
    private float[] compiledStaticVertices;
    private Vec3[] compiledTransformPositions;
    private boolean cachedFaceNormalValid;
    private float cachedFaceNormalX;
    private float cachedFaceNormalY;
    private float cachedFaceNormalZ;

    private static final float INV_16 = 0.0625F;
    private static final ThreadLocal<RenderScratch> RENDER_SCRATCH = ThreadLocal.withInitial(RenderScratch::new);

    public TexturedPolygon(PositionTextureVertex[] apositionTexturevertex)
    {
        this.invertNormal = false;
        this.vertexPositions = apositionTexturevertex;
        this.nVertices = apositionTexturevertex.length;
        this.iNormals = new ArrayList<>();
        this.normals = new float[0];
        this.hasTransformVertices = Arrays.stream(apositionTexturevertex).anyMatch(PositionTransformVertex.class::isInstance);
        this.renderVertexIndices = createRenderVertexIndices(nVertices);
    }

    public TexturedPolygon(PositionTextureVertex[] apositionTexturevertex, int par2, int par3, int par4, int par5, float par6, float par7)
    {
        this(apositionTexturevertex);
        float var8 = 0.0F / par6;
        float var9 = 0.0F / par7;
        apositionTexturevertex[0] = apositionTexturevertex[0].setTexturePosition(par4 / par6 - var8, par3 / par7 + var9);
        apositionTexturevertex[1] = apositionTexturevertex[1].setTexturePosition(par2 / par6 + var8, par3 / par7 + var9);
        apositionTexturevertex[2] = apositionTexturevertex[2].setTexturePosition(par2 / par6 + var8, par5 / par7 - var9);
        apositionTexturevertex[3] = apositionTexturevertex[3].setTexturePosition(par4 / par6 - var8, par5 / par7 - var9);
    }

    public void setNormals(float x, float y, float z)
    {
        normals = new float[]{x, y, z};
        geometryRevision++;
        GeometryRevision.changed();
    }

    public void setInvertNormal(boolean invertNormal)
    {
        this.invertNormal = invertNormal;
        geometryRevision++;
        GeometryRevision.changed();
    }

    public void flipFace()
    {
        PositionTextureVertex[] var1 = new PositionTextureVertex[this.vertexPositions.length];

        for(int var2 = 0; var2 < this.vertexPositions.length; ++var2)
        {
            var1[var2] = this.vertexPositions[this.vertexPositions.length - var2 - 1];
        }

        this.vertexPositions = var1;
        invalidateCompiledVertices();
    }

    public void setNormals(List<Vec3> vec)
    {
        iNormals = vec;
        externalNormals = true;
        geometryRevision++;
        GeometryRevision.changed();
    }

    public void draw(PoseStack.Pose pose, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        draw(pose, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, false);
    }

    public void draw(PoseStack.Pose pose, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, boolean glow)
    {
        draw(pose, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, glow, Long.MIN_VALUE);
    }

    void draw(PoseStack.Pose pose, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, boolean glow, long transformationSequence)
    {
        if (nVertices < 3)
            return;

        final Matrix4f positionMatrix = pose.pose();
        final Matrix3f normalMatrix = pose.normal();

        final int finalLight = glow ? LightTexture.FULL_BRIGHT : packedLight;

        // Normal direction flip
        final float normalSign = invertNormal ? -1F : 1F;

        final int perVertexNormalCount = iNormals.size();
        final boolean hasPerVertexNormals = perVertexNormalCount > 0;

        RenderScratch scratch = RENDER_SCRATCH.get();
        final Vector3f transformedNormal = scratch.normal;

        if (hasTransformVertices)
            refreshTransformedVertices(transformationSequence);

        if (!hasPerVertexNormals)
        {
            if (normals.length == 3)
            {
                transformedNormal.set(normals[0] * normalSign, normals[1] * normalSign, normals[2] * normalSign);
                normalMatrix.transform(transformedNormal);
            }
            else if (vertexPositions.length >= 3)
            {
                setFaceNormal(transformedNormal, normalMatrix, normalSign);
            }
            else
                return;
        }

        float[] staticVertices = getCompiledStaticVertices();
        // Incomplete legacy normal lists inherit the previous emitted normal.
        // Preserve their original emission order instead of changing that fallback.
        int[] indices = !glow && hasPerVertexNormals && perVertexNormalCount < nVertices
            ? getLegacyRenderVertexIndices() : renderVertexIndices;
        for (int vertexIndex : indices)
        {
            int dataIndex = vertexIndex * 5;
            emitVertex(positionMatrix, normalMatrix, transformedNormal, vertexConsumer,
                    packedOverlay, finalLight, red, green, blue, alpha, glow, normalSign,
                    perVertexNormalCount, hasPerVertexNormals, vertexIndex,
                    staticVertices[dataIndex], staticVertices[dataIndex + 1], staticVertices[dataIndex + 2],
                    staticVertices[dataIndex + 3], staticVertices[dataIndex + 4]);
        }
    }

    private float[] getCompiledStaticVertices()
    {
        if (compiledStaticVertices != null)
            return compiledStaticVertices;

        float[] data = new float[nVertices * 5];
        for (int vertexIndex = 0; vertexIndex < nVertices; vertexIndex++)
        {
            PositionTextureVertex vertex = vertexPositions[vertexIndex];
            int dataIndex = vertexIndex * 5;
            data[dataIndex] = (float)vertex.vector3D.x() * INV_16;
            data[dataIndex + 1] = (float)vertex.vector3D.y() * INV_16;
            data[dataIndex + 2] = (float)vertex.vector3D.z() * INV_16;
            data[dataIndex + 3] = vertex.texturePositionX;
            data[dataIndex + 4] = vertex.texturePositionY;
        }
        compiledStaticVertices = data;
        return data;
    }

    public void invalidateCompiledVertices()
    {
        geometryRevision++;
        GeometryRevision.changed();
        hasTransformVertices = false;
        for (PositionTextureVertex vertex : vertexPositions)
            hasTransformVertices |= vertex instanceof PositionTransformVertex;
        renderVertexIndices = createRenderVertexIndices(nVertices);
        legacyRenderVertexIndices = null;
        compiledStaticVertices = null;
        compiledTransformPositions = null;
        cachedFaceNormalValid = false;
    }

    /**
     * Compatibility path for geometry handed to external code. Public array elements,
     * vertex fields and retained list references cannot emit mutation notifications.
     * Internally owned model geometry never pays for this scan during normal rendering.
     */
    void observeExternalMutations()
    {
        boolean changed = observedVertices == null || observedVertices.length != vertexPositions.length || observedCount != nVertices;
        if (observedVertices == null || observedVertices.length != vertexPositions.length)
        {
            observedVertices = new PositionTextureVertex[vertexPositions.length];
            observedPositions = new Vec3[vertexPositions.length];
            observedUvs = new float[vertexPositions.length * 2];
        }
        for (int i = 0; i < vertexPositions.length; i++)
        {
            PositionTextureVertex vertex = vertexPositions[i];
            if (observedVertices[i] != vertex || !(vertex instanceof PositionTransformVertex) && observedPositions[i] != vertex.vector3D
                || Float.floatToIntBits(observedUvs[i * 2]) != Float.floatToIntBits(vertex.texturePositionX)
                || Float.floatToIntBits(observedUvs[i * 2 + 1]) != Float.floatToIntBits(vertex.texturePositionY))
                changed = true;
            observedVertices[i] = vertex;
            observedPositions[i] = vertex.vector3D;
            observedUvs[i * 2] = vertex.texturePositionX;
            observedUvs[i * 2 + 1] = vertex.texturePositionY;
        }
        observedCount = nVertices;
        if (changed) invalidateCompiledVertices();
    }

    /** Derived LOD geometry must not freeze legacy bone deformation. */
    public boolean isRigidLodGeometry()
    {
        return getClass() == TexturedPolygon.class && !hasTransformVertices && !invertNormal && iNormals.isEmpty();
    }

    /** A retained empty normals list may acquire normals without calling a setter. */
    public boolean isRigidGpuGeometry()
    {
        return isRigidLodGeometry() && !externalNormals;
    }

    public long geometryRevision()
    {
        return geometryRevision;
    }

    /**
     * Minecraft's entity buffers consume quads. Cache the immutable expansion of
     * triangles and polygon fans once. Pair adjacent fan triangles into a quad:
     * Minecraft's (0,1,2), (2,3,0) indices preserve the original triangles and winding,
     * without submitting four vertices for every triangle separately.
     */
    private static int[] createRenderVertexIndices(int vertexCount)
    {
        if (vertexCount < 3)
            return new int[0];
        if (vertexCount == 3)
            return new int[]{0, 1, 2, 2};
        if (vertexCount == 4)
            return new int[]{0, 1, 2, 3};

        int[] indices = new int[((vertexCount - 1) / 2) * 4];
        int outputIndex = 0;
        for (int vertexIndex = 1; vertexIndex < vertexCount - 1; vertexIndex += 2)
        {
            indices[outputIndex++] = 0;
            indices[outputIndex++] = vertexIndex;
            indices[outputIndex++] = vertexIndex + 1;
            indices[outputIndex++] = Math.min(vertexIndex + 2, vertexCount - 1);
        }
        return indices;
    }

    private int[] getLegacyRenderVertexIndices()
    {
        if (nVertices <= 4)
            return renderVertexIndices;
        if (legacyRenderVertexIndices == null)
        {
            legacyRenderVertexIndices = new int[(nVertices - 2) * 4];
            int outputIndex = 0;
            for (int vertexIndex = 1; vertexIndex < nVertices - 1; vertexIndex++)
            {
                legacyRenderVertexIndices[outputIndex++] = 0;
                legacyRenderVertexIndices[outputIndex++] = vertexIndex;
                legacyRenderVertexIndices[outputIndex++] = vertexIndex + 1;
                legacyRenderVertexIndices[outputIndex++] = vertexIndex + 1;
            }
        }
        return legacyRenderVertexIndices;
    }

    /**
     * Extruded rigid shapes also use transform vertices. Keep invoking their legacy
     * transformations, but retain positions and face normals while the resulting
     * immutable Vec3 instances are unchanged. This also observes groups added later
     * through the public legacy lists, without permanently classifying a part static.
     */
    private void refreshTransformedVertices(long transformationSequence)
    {
        if (compiledStaticVertices == null)
        {
            compiledStaticVertices = new float[nVertices * 5];
            compiledTransformPositions = new Vec3[nVertices];
        }
        for (int index = 0; index < nVertices; index++)
        {
            PositionTextureVertex vertex = vertexPositions[index];
            if (vertex instanceof PositionTransformVertex transformVertex)
            {
                if (transformationSequence == Long.MIN_VALUE)
                    transformVertex.setTransformation();
                else
                    transformVertex.setTransformation(transformationSequence);
            }
            Vec3 position = vertex.vector3D;
            int dataIndex = index * 5;
            if (compiledTransformPositions[index] != position)
            {
                compiledTransformPositions[index] = position;
                compiledStaticVertices[dataIndex] = (float)position.x * INV_16;
                compiledStaticVertices[dataIndex + 1] = (float)position.y * INV_16;
                compiledStaticVertices[dataIndex + 2] = (float)position.z * INV_16;
                cachedFaceNormalValid = false;
            }
            compiledStaticVertices[dataIndex + 3] = vertex.texturePositionX;
            compiledStaticVertices[dataIndex + 4] = vertex.texturePositionY;
        }
    }

    private void setFaceNormal(Vector3f transformedNormal, Matrix3f normalMatrix, float normalSign)
    {
        if (!cachedFaceNormalValid)
        {
            Vec3 vector0 = vertexPositions[0].vector3D;
            Vec3 vector1 = vertexPositions[1].vector3D;
            Vec3 vector2 = vertexPositions[2].vector3D;

            double edgeAX = vector1.x - vector0.x;
            double edgeAY = vector1.y - vector0.y;
            double edgeAZ = vector1.z - vector0.z;
            double edgeBX = vector1.x - vector2.x;
            double edgeBY = vector1.y - vector2.y;
            double edgeBZ = vector1.z - vector2.z;

            double normalX = edgeBY * edgeAZ - edgeBZ * edgeAY;
            double normalY = edgeBZ * edgeAX - edgeBX * edgeAZ;
            double normalZ = edgeBX * edgeAY - edgeBY * edgeAX;
            double length = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);

            if (length != 0D)
            {
                double inverseLength = 1D / length;
                normalX *= inverseLength;
                normalY *= inverseLength;
                normalZ *= inverseLength;
            }

            cachedFaceNormalX = (float)normalX;
            cachedFaceNormalY = (float)normalY;
            cachedFaceNormalZ = (float)normalZ;
            cachedFaceNormalValid = true;
        }

        transformedNormal.set(cachedFaceNormalX * normalSign, cachedFaceNormalY * normalSign, cachedFaceNormalZ * normalSign);
        normalMatrix.transform(transformedNormal);
    }

    private void emitVertex(Matrix4f positionMatrix, Matrix3f normalMatrix,
            Vector3f transformedNormal, VertexConsumer vertexConsumer,
            int packedOverlay, int finalLight, float red, float green, float blue, float alpha,
            boolean glow, float normalSign, int perVertexNormalCount, boolean hasPerVertexNormals,
            int vertexIndex, float localX, float localY, float localZ, float textureX, float textureY)
    {
        if (hasPerVertexNormals && !glow && vertexIndex < perVertexNormalCount)
        {
            Vec3 normal = iNormals.get(vertexIndex);
            transformedNormal.set((float)normal.x * normalSign, (float)normal.y * normalSign, (float)normal.z * normalSign);
            normalMatrix.transform(transformedNormal);
        }

        final float normalX = glow ? 0F : transformedNormal.x();
        final float normalY = glow ? 1F : transformedNormal.y();
        final float normalZ = glow ? 0F : transformedNormal.z();

        final float transformedX = positionMatrix.m00() * localX + positionMatrix.m10() * localY + positionMatrix.m20() * localZ + positionMatrix.m30();
        final float transformedY = positionMatrix.m01() * localX + positionMatrix.m11() * localY + positionMatrix.m21() * localZ + positionMatrix.m31();
        final float transformedZ = positionMatrix.m02() * localX + positionMatrix.m12() * localY + positionMatrix.m22() * localZ + positionMatrix.m32();

        vertexConsumer.addVertex(transformedX, transformedY, transformedZ)
            .setColor(red, green, blue, alpha)
            .setUv(textureX, textureY)
            .setOverlay(packedOverlay)
            .setLight(finalLight)
            .setNormal(normalX, normalY, normalZ);
    }

    private static final class RenderScratch
    {
        private final Vector3f normal = new Vector3f();
    }
}
