package com.flansmodultimate.client.render.gpu;

import java.util.Arrays;

/** Identity sequence. Only immutable copies are stored; a batch owns its reusable probe. */
final class GeometryKey
{
    final RigidGeometry[] geometries;
    final int[] paletteIndices;
    int count;
    private int hash = 1;

    GeometryKey(int capacity)
    {
        geometries = new RigidGeometry[capacity];
        paletteIndices = new int[capacity];
    }

    private GeometryKey(GeometryKey probe)
    {
        geometries = Arrays.copyOf(probe.geometries, probe.count);
        paletteIndices = Arrays.copyOf(probe.paletteIndices, probe.count);
        count = probe.count;
        hash = probe.hash;
    }

    void add(RigidGeometry geometry)
    {
        add(geometry, count);
    }

    void add(RigidGeometry geometry, int paletteIndex)
    {
        paletteIndices[count] = paletteIndex;
        geometries[count++] = geometry;
        hash = 31 * hash + System.identityHashCode(geometry);
        hash = 31 * hash + paletteIndex;
    }

    GeometryKey snapshot() { return new GeometryKey(this); }

    void clear()
    {
        Arrays.fill(geometries, 0, count, null);
        count = 0;
        hash = 1;
    }

    @Override
    public int hashCode()
    {
        return hash;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if (!(other instanceof GeometryKey key) || hash != key.hash || count != key.count)
            return false;

        for (int i = 0; i < count; i++)
            if (geometries[i] != key.geometries[i] || paletteIndices[i] != key.paletteIndices[i]) return false;

        return true;
    }
}
