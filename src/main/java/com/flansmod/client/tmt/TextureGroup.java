package com.flansmod.client.tmt;

import com.flansmodultimate.client.render.gpu.GeometryRevision;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

@SuppressWarnings({"unused", "java:S1104"})
public class TextureGroup
{
    public List<TexturedPolygon> poly;
    /** Legacy per-group texture, kept for compiled models that call setGroupTexture; renderers bind the model texture. */
    public String texture;

    public TextureGroup()
    {
        poly = new PolygonList();
        texture = "";
    }

    public void addPoly(TexturedPolygon polygon)
    {
        poly.add(polygon);
    }

    /** AbstractList routes iterators, sublists, sort and replaceAll through these mutators too. */
    private static final class PolygonList extends AbstractList<TexturedPolygon> implements RandomAccess
    {
        private final ArrayList<TexturedPolygon> values = new ArrayList<>();

        @Override
        public int size()
        {
            return values.size();
        }

        @Override
        public TexturedPolygon get(int index)
        {
            return values.get(index);
        }

        @Override
        public TexturedPolygon set(int index, TexturedPolygon value)
        {
            TexturedPolygon old = values.set(index, value);
            GeometryRevision.changed();
            return old;
        }

        @Override
        public void add(int index, TexturedPolygon value)
        {
            values.add(index, value);
            modCount++;
            GeometryRevision.changed();
        }
        @Override
        public TexturedPolygon remove(int index)
        {
            TexturedPolygon old = values.remove(index);
            modCount++;
            GeometryRevision.changed();
            return old;
        }
    }

}
