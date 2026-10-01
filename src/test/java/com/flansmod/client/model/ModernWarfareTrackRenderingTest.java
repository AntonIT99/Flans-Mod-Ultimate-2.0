package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.TexturedPolygon;
import com.flansmodultimate.client.model.ModelBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ModernWarfareTrackRenderingTest
{
    @ParameterizedTest
    @CsvSource({"Leopard2A6, leopard2a6, 15", "Abrams, m1a2, 13"})
    void everyTrackFrameUsesPaintedTextureTiles(String modelName, String skinName, int treadDepth) throws Exception
    {
        Path pack = Path.of("src/officialpacks");
        BufferedImage skin = ImageIO.read(pack.resolve(
            "resources/assets/flansmod/textures/skins/" + skinName + ".png").toFile());
        String source = Files.readString(pack.resolve("java/com/flansmod/client/model/mw/Model" + modelName + ".java"));
        var tiles = Pattern.compile("(?:left|right)AnimTrackModel\\[(\\d)]\\[\\d] = new ModelRendererTurbo\\(this, (\\d+), (\\d+),")
            .matcher(source);
        int[] frameParts = new int[3];
        while (tiles.find())
        {
            int frame = Integer.parseInt(tiles.group(1));
            int x = Integer.parseInt(tiles.group(2)), y = Integer.parseInt(tiles.group(3));
            // Every authored tread piece has a top face at least four pixels wide.
            // The original second and third banks were wholly transparent.
            boolean painted = false;
            for (int u = x + treadDepth; u < x + treadDepth + 4; u++)
                for (int v = y; v < y + treadDepth; v++)
                    painted |= (skin.getRGB(u, v) >>> 24) >= 128;
            assertTrue(painted, "Transparent " + modelName + " track tile: frame=" + frame + ", x=" + x + ", y=" + y);
            frameParts[frame]++;
        }
        assertArrayEquals(new int[]{18, 18, 18}, frameParts, "Both sides must retain all three complete frames");
        assertTrue(source.contains("TrackFrameUvAnimation.apply(leftAnimTrackModel, textureX)"));
        assertTrue(source.contains("TrackFrameUvAnimation.apply(rightAnimTrackModel, textureX)"));
    }

    @ParameterizedTest
    @CsvSource({"Leopard2A6, leopard2a6", "Abrams, m1a2"})
    void scrollingFramesMoveThePaintedTreadWithoutChangingSurfaceOrWinding(String modelName, String skinName) throws Exception
    {
        Path pack = Path.of("src/officialpacks");
        BufferedImage skin = ImageIO.read(pack.resolve(
            "resources/assets/flansmod/textures/skins/" + skinName + ".png").toFile());
        String source = Files.readString(pack.resolve("java/com/flansmod/client/model/mw/Model" + modelName + ".java"));
        int[] changed = new int[3];
        // Rebuild every authored rectangular tread piece, rather than a synthetic UV-only face.
        var boxes = Pattern.compile("((?:left|right)AnimTrackModel)\\[0]\\[(\\d)]\\.addShapeBox\\(0F, 0F, 0F, (\\d+), (\\d+), (\\d+),")
            .matcher(source);
        int pieces = 0;
        while (boxes.find())
        {
            var tile = Pattern.compile(Pattern.quote(boxes.group(1) + "[0][" + boxes.group(2) + "]")
                + " = new ModelRendererTurbo\\(this, (\\d+), (\\d+),").matcher(source);
            assertTrue(tile.find());
            ModelRendererTurbo[][] frames = new ModelRendererTurbo[3][1];
            for (int frame = 0; frame < 3; frame++)
            {
                var part = new ModelRendererTurbo(new ModelBase() {}, Integer.parseInt(tile.group(1)),
                    Integer.parseInt(tile.group(2)), skin.getWidth(), skin.getHeight());
                part.addBox(0, 0, 0, Integer.parseInt(boxes.group(3)), Integer.parseInt(boxes.group(4)), Integer.parseInt(boxes.group(5)));
                part.doMirror(false, true, true); // Model constructors call flipAll before applying UV phases.
                frames[frame][0] = part;
            }
            List<TexturedPolygon> original = List.copyOf(frames[0][0].getTextureGroup().poly);
            TrackFrameUvAnimation.apply(frames, skin.getWidth());
            assertEquals(original, frames[0][0].getTextureGroup().poly, "Frame zero stays untouched");
            for (int frame = 1; frame < 3; frame++)
            {
                List<TexturedPolygon> shifted = frames[frame][0].getTextureGroup().poly;
                assertEquals(area(original), area(shifted), 1E-4, "Wrapping must preserve the complete belt surface");
                int index = 0;
                for (TexturedPolygon face : original)
                {
                    Vec3 normal = normal(face).normalize();
                    List<TexturedPolygon> fragments = new ArrayList<>();
                    double covered = 0;
                    while (covered < area(List.of(face)) - 1E-4)
                    {
                        TexturedPolygon fragment = shifted.get(index++);
                        fragments.add(fragment);
                        covered += area(List.of(fragment));
                        assertTrue(normal.dot(normal(fragment).normalize()) > 0.999, "UV seams must preserve face winding");
                        assertUvBounds(face, fragment);
                    }
                    assertEquals(area(List.of(face)), covered, 1E-4);
                    Vec3 center = face.vertexPositions[0].vector3D.lerp(face.vertexPositions[2].vector3D, 0.5);
                    float originalU = uvAt(List.of(face), center)[0];
                    float expectedU = originalU;
                    float minU = Float.POSITIVE_INFINITY, maxU = Float.NEGATIVE_INFINITY;
                    for (var vertex : face.vertexPositions)
                    {
                        minU = Math.min(minU, vertex.texturePositionX);
                        maxU = Math.max(maxU, vertex.texturePositionX);
                    }
                    for (var vertex : face.vertexPositions)
                    {
                        double dx = vertex.vector3D.x - face.vertexPositions[0].vector3D.x;
                        if (Math.abs(dx) > 1E-6)
                        {
                            double direction = (vertex.texturePositionX - face.vertexPositions[0].texturePositionX) / dx;
                            expectedU = originalU - Math.copySign((float)frame / skin.getWidth(), (float)direction);
                            break;
                        }
                    }
                    if (expectedU < minU) expectedU += maxU - minU;
                    if (expectedU > maxU) expectedU -= maxU - minU;
                    assertEquals(expectedU, uvAt(fragments, center)[0], 1E-6,
                        "All belt surfaces must scroll in +local X, including faces with reversed U");
                    for (int u = 0; u < 11; u++)
                        for (int v = 0; v < 5; v++)
                        {
                            Vec3 point = face.vertexPositions[0].vector3D
                                .add(face.vertexPositions[1].vector3D.subtract(face.vertexPositions[0].vector3D).scale((u + 0.5) / 11))
                                .add(face.vertexPositions[3].vector3D.subtract(face.vertexPositions[0].vector3D).scale((v + 0.5) / 5));
                            int before = sample(skin, List.of(face), point), after = sample(skin, fragments, point);
                            if ((before >>> 24) >= 128)
                                assertTrue((after >>> 24) >= 128, "Animation must not move painted treads into transparent atlas space");
                            if (before != after) changed[frame]++;
                            if (frame == 2 && after != sample(skin, frames[1][0].getTextureGroup().poly, point)) changed[0]++;
                        }
                }
                assertEquals(shifted.size(), index);
            }
            pieces++;
        }
        assertEquals(18, pieces);
        for (int count : changed) assertTrue(count > 100, "Each pair of phases must visibly move the tread pattern");
    }

    private static void assertUvBounds(TexturedPolygon original, TexturedPolygon fragment)
    {
        float minU = Float.POSITIVE_INFINITY, maxU = Float.NEGATIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
        for (var vertex : original.vertexPositions)
        {
            minU = Math.min(minU, vertex.texturePositionX); maxU = Math.max(maxU, vertex.texturePositionX);
            minV = Math.min(minV, vertex.texturePositionY); maxV = Math.max(maxV, vertex.texturePositionY);
        }
        for (var vertex : fragment.vertexPositions)
        {
            assertTrue(vertex.texturePositionX >= minU - 1E-7 && vertex.texturePositionX <= maxU + 1E-7);
            assertTrue(vertex.texturePositionY >= minV - 1E-7 && vertex.texturePositionY <= maxV + 1E-7);
        }
    }

    private static Vec3 normal(TexturedPolygon face)
    {
        var vertices = face.vertexPositions;
        return vertices[1].vector3D.subtract(vertices[0].vector3D)
            .cross(vertices[2].vector3D.subtract(vertices[0].vector3D));
    }

    private static double area(List<TexturedPolygon> faces)
    {
        double area = 0;
        for (TexturedPolygon face : faces)
            for (int i = 1; i < face.nVertices - 1; i++)
                area += face.vertexPositions[i].vector3D.subtract(face.vertexPositions[0].vector3D)
                    .cross(face.vertexPositions[i + 1].vector3D.subtract(face.vertexPositions[0].vector3D)).length() * 0.5;
        return area;
    }

    private static int sample(BufferedImage skin, List<TexturedPolygon> faces, Vec3 point)
    {
        float[] uv = uvAt(faces, point);
        return skin.getRGB((int)(uv[0] * skin.getWidth()), (int)(uv[1] * skin.getHeight()));
    }

    private static float[] uvAt(List<TexturedPolygon> faces, Vec3 point)
    {
        for (TexturedPolygon face : faces)
            for (int i = 1; i < face.nVertices - 1; i++)
            {
                var a = face.vertexPositions[0]; var b = face.vertexPositions[i]; var c = face.vertexPositions[i + 1];
                Vec3 ab = b.vector3D.subtract(a.vector3D), ac = c.vector3D.subtract(a.vector3D), ap = point.subtract(a.vector3D);
                if (Math.abs(ab.cross(ac).normalize().dot(ap)) > 1E-5) continue;
                double determinant = ab.dot(ab) * ac.dot(ac) - ab.dot(ac) * ab.dot(ac);
                double u = (ap.dot(ab) * ac.dot(ac) - ap.dot(ac) * ab.dot(ac)) / determinant;
                double v = (ap.dot(ac) * ab.dot(ab) - ap.dot(ab) * ab.dot(ac)) / determinant;
                if (u < -1E-5 || v < -1E-5 || u + v > 1 + 1E-5) continue;
                float texU = (float)(a.texturePositionX + u * (b.texturePositionX - a.texturePositionX) + v * (c.texturePositionX - a.texturePositionX));
                float texV = (float)(a.texturePositionY + u * (b.texturePositionY - a.texturePositionY) + v * (c.texturePositionY - a.texturePositionY));
                return new float[]{texU, texV};
            }
        fail("A UV wrap left a gap in the belt geometry");
        return new float[2];
    }
}
