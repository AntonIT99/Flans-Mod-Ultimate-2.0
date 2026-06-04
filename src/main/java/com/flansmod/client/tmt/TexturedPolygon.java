package com.flansmod.client.tmt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Setter;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "SameParameterValue", "java:S1104"})
public class TexturedPolygon
{
    public PositionTextureVertex[] vertexPositions;
    public int nVertices;

    @Setter
    private boolean invertNormal;
    private float[] normals;
    private List<Vec3> iNormals;

    private static final float INV_16 = 0.0625F;

    public TexturedPolygon(PositionTextureVertex[] apositionTexturevertex)
    {
        this.invertNormal = false;
        this.vertexPositions = apositionTexturevertex;
        this.nVertices = apositionTexturevertex.length;
        this.iNormals = new ArrayList<>();
        this.normals = new float[0];
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
    }

    public void flipFace()
    {
        PositionTextureVertex[] var1 = new PositionTextureVertex[this.vertexPositions.length];

        for(int var2 = 0; var2 < this.vertexPositions.length; ++var2)
        {
            var1[var2] = this.vertexPositions[this.vertexPositions.length - var2 - 1];
        }

        this.vertexPositions = var1;
    }

    public void setNormals(List<Vec3> vec)
    {
        iNormals = vec;
    }

    public void draw(PoseStack.Pose pose, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        draw(pose, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, false);
    }

    public void draw(PoseStack.Pose pose, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, boolean glow)
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

        final Vector3f transformedNormal = new Vector3f();
        final Vector4f transformedPos = new Vector4f();

        // If we DON'T have per-vertex normals, compute one base normal for the whole face
        if (!hasPerVertexNormals)
        {
            if (normals.length == 3)
            {
                transformedNormal.set(normals[0] * normalSign, normals[1] * normalSign, normals[2] * normalSign);
                normalMatrix.transform(transformedNormal);
            }
            else if (vertexPositions.length >= 3)
            {
                Vec3 edgeA = vertexPositions[1].vector3D.subtract(vertexPositions[0].vector3D);
                Vec3 edgeB = vertexPositions[1].vector3D.subtract(vertexPositions[2].vector3D);
                Vec3 faceNormal = edgeB.cross(edgeA).normalize();

                transformedNormal.set((float) faceNormal.x * normalSign, (float) faceNormal.y * normalSign, (float) faceNormal.z * normalSign);
                normalMatrix.transform(transformedNormal);
            }
            else
                return;
        }

        // Minecraft entity render types are quad buffers; emulate old triangle/polygon modes with degenerate quads.
        if (nVertices == 3)
        {
            emitTriangleAsQuad(positionMatrix, normalMatrix, transformedNormal, transformedPos, vertexConsumer,
                    packedOverlay, finalLight, red, green, blue, alpha, glow, normalSign,
                    perVertexNormalCount, hasPerVertexNormals, 0, 1, 2);
        }
        else if (nVertices == 4)
        {
            for (int vertexIndex = 0; vertexIndex < nVertices; vertexIndex++)
            {
                emitVertex(positionMatrix, normalMatrix, transformedNormal, transformedPos, vertexConsumer,
                        packedOverlay, finalLight, red, green, blue, alpha, glow, normalSign,
                        perVertexNormalCount, hasPerVertexNormals, vertexIndex);
            }
        }
        else
        {
            for (int vertexIndex = 1; vertexIndex < nVertices - 1; vertexIndex++)
            {
                emitTriangleAsQuad(positionMatrix, normalMatrix, transformedNormal, transformedPos, vertexConsumer,
                        packedOverlay, finalLight, red, green, blue, alpha, glow, normalSign,
                        perVertexNormalCount, hasPerVertexNormals, 0, vertexIndex, vertexIndex + 1);
            }
        }
    }

    private void emitTriangleAsQuad(Matrix4f positionMatrix, Matrix3f normalMatrix,
                                    Vector3f transformedNormal, Vector4f transformedPos, VertexConsumer vertexConsumer,
                                    int packedOverlay, int finalLight, float red, float green, float blue, float alpha,
                                    boolean glow, float normalSign, int perVertexNormalCount, boolean hasPerVertexNormals,
                                    int vertexIndex0, int vertexIndex1, int vertexIndex2)
    {
        emitVertex(positionMatrix, normalMatrix, transformedNormal, transformedPos, vertexConsumer,
                packedOverlay, finalLight, red, green, blue, alpha, glow, normalSign,
                perVertexNormalCount, hasPerVertexNormals, vertexIndex0);
        emitVertex(positionMatrix, normalMatrix, transformedNormal, transformedPos, vertexConsumer,
                packedOverlay, finalLight, red, green, blue, alpha, glow, normalSign,
                perVertexNormalCount, hasPerVertexNormals, vertexIndex1);
        emitVertex(positionMatrix, normalMatrix, transformedNormal, transformedPos, vertexConsumer,
                packedOverlay, finalLight, red, green, blue, alpha, glow, normalSign,
                perVertexNormalCount, hasPerVertexNormals, vertexIndex2);
        emitVertex(positionMatrix, normalMatrix, transformedNormal, transformedPos, vertexConsumer,
                packedOverlay, finalLight, red, green, blue, alpha, glow, normalSign,
                perVertexNormalCount, hasPerVertexNormals, vertexIndex2);
    }

    private void emitVertex(Matrix4f positionMatrix, Matrix3f normalMatrix,
            Vector3f transformedNormal, Vector4f transformedPos, VertexConsumer vertexConsumer,
            int packedOverlay, int finalLight, float red, float green, float blue, float alpha,
            boolean glow, float normalSign, int perVertexNormalCount, boolean hasPerVertexNormals,
            int vertexIndex)
    {
        PositionTextureVertex vertex = vertexPositions[vertexIndex];

        if (vertex instanceof PositionTransformVertex transformVertex)
            transformVertex.setTransformation();

        if (hasPerVertexNormals && !glow && vertexIndex < perVertexNormalCount)
        {
            Vec3 normal = iNormals.get(vertexIndex);
            transformedNormal.set((float)normal.x * normalSign, (float)normal.y * normalSign, (float)normal.z * normalSign);
            normalMatrix.transform(transformedNormal);
        }

        final float normalX = glow ? 0F : transformedNormal.x();
        final float normalY = glow ? 1F : transformedNormal.y();
        final float normalZ = glow ? 0F : transformedNormal.z();

        final float localX = (float)vertex.vector3D.x() * INV_16;
        final float localY = (float)vertex.vector3D.y() * INV_16;
        final float localZ = (float)vertex.vector3D.z() * INV_16;

        transformedPos.set(localX, localY, localZ, 1F);
        positionMatrix.transform(transformedPos);

        vertexConsumer.vertex(transformedPos.x(), transformedPos.y(), transformedPos.z(), red, green, blue, alpha, vertex.texturePositionX, vertex.texturePositionY, packedOverlay, finalLight, normalX, normalY, normalZ);
    }
}
