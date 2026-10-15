package com.flansmodultimate.platform.render;

import com.flansmod.client.tmt.PositionTextureVertex;
import com.flansmod.client.tmt.TexturedPolygon;
import com.flansmodultimate.client.render.gpu.RigidGeometry;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeshUploadBufferTest
{
    @Test
    void interruptedUploadCanStartAgainAfterReload()
    {
        try (MeshUploadBuffer upload = new MeshUploadBuffer(256))
        {
            upload.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            upload.discard();
            BufferBuilder builder = upload.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            quad().draw(new PoseStack().last(), builder, 17, 23, 1, 1, 1, 1);
            // Forge 1.20.1 hands back the rendered buffer itself; the GL upload needs a context.
            var rendered = builder.end();
            assertEquals(4, rendered.drawState().vertexCount());
            rendered.release();
        }
    }

    private static RigidGeometry quad()
    {
        return new RigidGeometry(new TexturedPolygon[]{new TexturedPolygon(new PositionTextureVertex[]{new PositionTextureVertex(0, 0, 0, 0, 0), new PositionTextureVertex(16, 0, 0, 1, 0),
            new PositionTextureVertex(16, 16, 0, 1, 1), new PositionTextureVertex(0, 16, 0, 0, 1)})});
    }
}
