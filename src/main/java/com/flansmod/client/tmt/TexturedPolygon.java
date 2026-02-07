package com.flansmod.client.tmt;

import org.lwjgl.opengl.GL11;

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

        for (int vertexIndex = 0; vertexIndex < nVertices; vertexIndex++)
        {
            PositionTextureVertex vertex = vertexPositions[vertexIndex];

            if (vertex instanceof PositionTransformVertex transformVertex)
                transformVertex.setTransformation();

            // If we have per-vertex normals and we're not glowing, transform the normal for this vertex
            if (hasPerVertexNormals && !glow && vertexIndex < perVertexNormalCount)
            {
                Vec3 normal = iNormals.get(vertexIndex);
                transformedNormal.set((float) normal.x * normalSign, (float) normal.y * normalSign, (float) normal.z * normalSign);
                normalMatrix.transform(transformedNormal);
            }

            // If glow is on, force a constant normal (as your original code effectively did)
            final float normalX = glow ? 0F : transformedNormal.x();
            final float normalY = glow ? 1F : transformedNormal.y();
            final float normalZ = glow ? 0F : transformedNormal.z();

            final float localX = (float) vertex.vector3D.x() * INV_16;
            final float localY = (float) vertex.vector3D.y() * INV_16;
            final float localZ = (float) vertex.vector3D.z() * INV_16;

            transformedPos.set(localX, localY, localZ, 1F);
            positionMatrix.transform(transformedPos);

            vertexConsumer.vertex(transformedPos.x(), transformedPos.y(), transformedPos.z(), red, green, blue, alpha, vertex.texturePositionX, vertex.texturePositionY, packedOverlay, finalLight, normalX, normalY, normalZ);
        }
    }

    @Deprecated
    public void draw(TmtTessellator tessellator, float f)
    {
        if(nVertices == 3)
            tessellator.startDrawing(GL11.GL_TRIANGLES);
        else if(nVertices == 4)
            tessellator.startDrawingQuads();
        else
            tessellator.startDrawing(GL11.GL_POLYGON);

        if(iNormals.isEmpty())
        {
            if(normals.length == 3)
            {
                if(invertNormal)
                {
                    tessellator.setNormal(-normals[0], -normals[1], -normals[2]);
                }
                else
                {
                    tessellator.setNormal(normals[0], normals[1], normals[2]);
                }
            }
            else if(vertexPositions.length >= 3)
            {
                Vec3 vec3d = vertexPositions[1].vector3D.subtract(vertexPositions[0].vector3D);
                Vec3 vec31 = vertexPositions[1].vector3D.subtract(vertexPositions[2].vector3D);
                Vec3 vec32 = vec31.cross(vec3d).normalize();

                if(invertNormal)
                {
                    tessellator.setNormal(-(float)vec32.x, -(float)vec32.y, -(float)vec32.z);
                }
                else
                {
                    tessellator.setNormal((float)vec32.x, (float)vec32.y, (float)vec32.z);
                }
            }
            else
            {
                return;
            }
        }
        for(int i = 0; i < nVertices; i++)
        {
            PositionTextureVertex positionTexturevertex = vertexPositions[i];
            if(positionTexturevertex instanceof PositionTransformVertex positionTransformVertex)
                positionTransformVertex.setTransformation();
            if(i < iNormals.size())
            {
                if(invertNormal)
                {
                    tessellator.setNormal(-(float)iNormals.get(i).x, -(float)iNormals.get(i).y, -(float)iNormals.get(i).z);
                }
                else
                {
                    tessellator.setNormal((float)iNormals.get(i).x, (float)iNormals.get(i).y, (float)iNormals.get(i).z);
                }
            }
            tessellator.addVertexWithUVW((float)positionTexturevertex.vector3D.x * f, (float)positionTexturevertex.vector3D.y * f, (float)positionTexturevertex.vector3D.z * f, positionTexturevertex.texturePositionX, positionTexturevertex.texturePositionY, positionTexturevertex.texturePositionW);
        }

        tessellator.draw();
    }
}
