package com.wolffsmod.api.client.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.world.phys.Vec3;

public class TexturedQuad
{
    public PositionTextureVertex[] vertexPositions;
    public int nVertices;
    private boolean invertNormal;

    public TexturedQuad(PositionTextureVertex[] vertices)
    {
        this.vertexPositions = vertices;
        this.nVertices = vertices.length;
    }

    public TexturedQuad(PositionTextureVertex[] vertices, int texcoordU1, int texcoordV1, int texcoordU2, int texcoordV2, float textureWidth, float textureHeight)
    {
        this(vertices);
        float f = 0.0F / textureWidth;
        float f1 = 0.0F / textureHeight;
        vertices[0] = vertices[0].setTexturePosition(texcoordU2 / textureWidth - f, texcoordV1 / textureHeight + f1);
        vertices[1] = vertices[1].setTexturePosition(texcoordU1 / textureWidth + f, texcoordV1 / textureHeight + f1);
        vertices[2] = vertices[2].setTexturePosition(texcoordU1 / textureWidth + f, texcoordV2 / textureHeight - f1);
        vertices[3] = vertices[3].setTexturePosition(texcoordU2 / textureWidth - f, texcoordV2 / textureHeight - f1);
    }

    public void flipFace()
    {
        PositionTextureVertex[] apositiontexturevertex = new PositionTextureVertex[this.vertexPositions.length];

        for (int i = 0; i < this.vertexPositions.length; ++i)
        {
            apositiontexturevertex[i] = this.vertexPositions[this.vertexPositions.length - i - 1];
        }

        this.vertexPositions = apositiontexturevertex;
    }

    /**
     * Draw this primitive. This is typically called only once as the generated drawing instructions are saved by the
     * renderer and reused later.
     */
    @OnlyIn(Dist.CLIENT)
    public void draw(BufferBuilder renderer, float scale)
    {
        Vec3 vec3 = this.vertexPositions[1].vector3D.multiply(-1, -1, -1).add(this.vertexPositions[0].vector3D);
        Vec3 vec31 = this.vertexPositions[1].vector3D.multiply(-1, -1, -1).add(this.vertexPositions[2].vector3D);
        Vec3 vec32 = vec31.cross(vec3).normalize();
        float f = (float)vec32.x;
        float f1 = (float)vec32.y;
        float f2 = (float)vec32.z;

        if (this.invertNormal)
        {
            f = -f;
            f1 = -f1;
            f2 = -f2;
        }

        VertexFormatElement position3f = new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION,3);
        VertexFormatElement texture2f = new VertexFormatElement(1, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);
        VertexFormatElement normal3b = new VertexFormatElement(2, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 3);
        VertexFormatElement padding1b = new VertexFormatElement(3, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.PADDING, 1);
        ImmutableMap<String, VertexFormatElement> elementMapping = ImmutableMap.of(
                "position", position3f,
                "texture", texture2f,
                "normal", normal3b,
                "padding", padding1b
        );
        VertexFormat oldModelPositionTexNormal = new VertexFormat(elementMapping);
        renderer.begin(VertexFormat.Mode.QUADS, oldModelPositionTexNormal);

        for (int i = 0; i < 4; ++i)
        {
            PositionTextureVertex positiontexturevertex = this.vertexPositions[i];
            renderer.vertex(positiontexturevertex.vector3D.x * (double)scale, positiontexturevertex.vector3D.y * (double)scale, positiontexturevertex.vector3D.z * (double)scale)
                    .uv(positiontexturevertex.texturePositionX, positiontexturevertex.texturePositionY)
                    .normal(f, f1, f2)
                    .endVertex();
        }

        Tessellator.getInstance().draw();
    }
}