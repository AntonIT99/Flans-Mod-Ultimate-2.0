package com.wolffsarmormod.client.tmt;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TexturedPolygon
{
    public PositionTextureVertex[] vertexPositions;
    public int nVertices;

    private boolean invertNormal;
    private float[] normals;
    private List<Vec3> iNormals;

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

    public void setInvertNormal(boolean isSet)
    {
        invertNormal = isSet;
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

    public void draw(PoseStack.Pose pPose, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha)
    {
        Matrix4f matrix4f = pPose.pose();
        Matrix3f matrix3f = pPose.normal();
        Vector3f vector3f = new Vector3f();

        if (iNormals.isEmpty())
        {
            if (normals.length == 3)
            {
                if (invertNormal)
                {
                    vector3f = matrix3f.transform(new Vector3f(-normals[0], -normals[1], -normals[2]));
                }
                else
                {
                    vector3f = matrix3f.transform(new Vector3f(normals[0], normals[1], normals[2]));
                }
            }
            else if (vertexPositions.length >= 3)
            {
                Vec3 vec3d = vertexPositions[1].vector3D.subtract(vertexPositions[0].vector3D);
                Vec3 vec31 = vertexPositions[1].vector3D.subtract(vertexPositions[2].vector3D);
                Vec3 vec32 = vec31.cross(vec3d).normalize();

                if (invertNormal)
                {
                    vector3f = matrix3f.transform(new Vector3f((float) -vec32.x, (float) -vec32.y, (float) -vec32.z));
                }
                else
                {
                    vector3f = matrix3f.transform(new Vector3f((float) vec32.x, (float) vec32.y, (float) vec32.z));
                }
            }
            else
            {
                return;
            }
        }

        for (int i = 0; i < nVertices; i++)
        {
            PositionTextureVertex positionTexturevertex = vertexPositions[i];

            if (positionTexturevertex instanceof PositionTransformVertex positionTransformVertex)
                positionTransformVertex.setTransformation();

            if (i < iNormals.size())
            {
                if(invertNormal)
                {
                    vector3f = matrix3f.transform(new Vector3f((float) -iNormals.get(i).x, (float) -iNormals.get(i).y, (float) -iNormals.get(i).z));
                }
                else
                {
                    vector3f = matrix3f.transform(new Vector3f((float) iNormals.get(i).x, (float) iNormals.get(i).y, (float) iNormals.get(i).z));
                }
            }

            float f = vector3f.x();
            float f1 = vector3f.y();
            float f2 = vector3f.z();
            float f3 = (float) positionTexturevertex.vector3D.x() / 16.0F;
            float f4 = (float) positionTexturevertex.vector3D.y() / 16.0F;
            float f5 = (float) positionTexturevertex.vector3D.z() / 16.0F;

            Vector4f vector4f = matrix4f.transform(new Vector4f(f3, f4, f5, 1.0F));
            pVertexConsumer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), pRed, pGreen, pBlue, pAlpha, positionTexturevertex.texturePositionX, positionTexturevertex.texturePositionY, pPackedOverlay, pPackedLight, f, f1, f2);
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
