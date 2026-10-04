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

    /**
     * Adds a polygon its own part just built. Only that part reads this group, and appending marks it to
     * re-read its groups, so the global geometry epoch stays put: bumping it makes every part of every
     * model re-validate and drops their render caches, a visible hitch whenever a model builds geometry.
     * A list replaced by outside code still notifies as before.
     */
    void addOwnedPoly(TexturedPolygon polygon)
    {
        if (poly instanceof PolygonList list)
            list.addOwned(polygon);
        else
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

        private void addOwned(TexturedPolygon value)
        {
            values.add(value);
            modCount++;
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
