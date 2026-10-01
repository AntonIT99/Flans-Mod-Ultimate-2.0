package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.PositionTextureVertex;
import com.flansmod.client.tmt.TexturedPolygon;

import java.util.ArrayList;
import java.util.List;

/** Builds legacy texture-swap frames from one painted bank of rectangular tread boxes. */
public final class TrackFrameUvAnimation
{
    private TrackFrameUvAnimation() {}

    /**
     * Each frame must initially use the same painted UVs, with local X along the belt.
     * Call after model mirroring: the new seam vertices use the final box positions.
     * One pixel per frame matches the three-pixel tread pattern in the MW tank skins.
     * This prepares static geometry once; the vehicle renderer still selects frames.
     */
    public static void apply(ModelRendererTurbo[][] frames, int textureWidth)
    {
        for (int frame = 1; frame < frames.length; frame++)
            for (ModelRendererTurbo part : frames[frame])
            {
                var group = part.getTextureGroup();
                List<TexturedPolygon> animated = new ArrayList<>();
                for (TexturedPolygon face : group.poly)
                    scroll(face, (float)frame / textureWidth, animated);
                group.poly.clear();
                group.poly.addAll(animated);
            }
    }

    private static void scroll(TexturedPolygon face, float phase, List<TexturedPolygon> output)
    {
        PositionTextureVertex[] vertices = face.vertexPositions;
        float minU = Float.POSITIVE_INFINITY, maxU = Float.NEGATIVE_INFINITY;
        double direction = 0;
        for (PositionTextureVertex vertex : vertices)
        {
            minU = Math.min(minU, vertex.texturePositionX);
            maxU = Math.max(maxU, vertex.texturePositionX);
            double dx = vertex.vector3D.x - vertices[0].vector3D.x;
            if (Math.abs(dx) > 1E-6)
                direction = (vertex.texturePositionX - vertices[0].texturePositionX) / dx;
        }
        // End caps do not run along the belt. These models use ordinary box normals.
        if (direction == 0 || maxU <= minU)
        {
            output.add(face);
            return;
        }

        float span = maxU - minU;
        // Increasing frames move the tread in +local X (forward on the upper run).
        // UV sampling moves in the opposite direction to the visible pattern.
        float shift = -Math.copySign(phase, (float)direction) % span;
        if (shift < 0) shift += span;
        if (shift == 0)
        {
            output.add(face);
            return;
        }
        float seam = maxU - shift;
        // Split at the wrap instead of interpolating across an atlas tile boundary.
        // Keep winding and full face coverage, including the thin wrapped strip.
        clip(vertices, seam, true, shift, output);
        clip(vertices, seam, false, shift - span, output);
    }

    private static void clip(PositionTextureVertex[] vertices, float seam, boolean below,
                             float shift, List<TexturedPolygon> output)
    {
        List<PositionTextureVertex> clipped = new ArrayList<>(4);
        PositionTextureVertex previous = vertices[vertices.length - 1];
        boolean previousInside = below ? previous.texturePositionX <= seam : previous.texturePositionX >= seam;
        for (PositionTextureVertex current : vertices)
        {
            boolean inside = below ? current.texturePositionX <= seam : current.texturePositionX >= seam;
            if (inside != previousInside)
            {
                float fraction = (seam - previous.texturePositionX) / (current.texturePositionX - previous.texturePositionX);
                clipped.add(new PositionTextureVertex(previous.vector3D.lerp(current.vector3D, fraction),
                    seam + shift, previous.texturePositionY + fraction * (current.texturePositionY - previous.texturePositionY)));
            }
            if (inside)
                clipped.add(current.setTexturePosition(current.texturePositionX + shift, current.texturePositionY));
            previous = current;
            previousInside = inside;
        }
        if (clipped.size() >= 3)
            output.add(new TexturedPolygon(clipped.toArray(PositionTextureVertex[]::new)));
    }
}
