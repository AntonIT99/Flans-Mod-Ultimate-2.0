package com.flansmodultimate.client.render.gpu;

import java.util.LinkedHashMap;

/** Render-thread LRU. Eviction/clear own resource disposal, including replacement. */
final class MeshCache<T extends MeshCache.Resource>
{
    interface Resource { long bytes(); void close(); }
    private final LinkedHashMap<GeometryKey, T> entries = new LinkedHashMap<>(64, 0.75F, true);
    private final long maximumBytes;
    private final int maximumEntries;
    private long bytes;

    MeshCache(long maximumBytes, int maximumEntries)
    {
        this.maximumBytes = maximumBytes;
        this.maximumEntries = maximumEntries;
    }

    T get(GeometryKey probe) { return entries.get(probe); }

    void put(GeometryKey probe, T resource)
    {
        T previous = entries.remove(probe);
        if (previous != null) { bytes -= previous.bytes(); previous.close(); }
        reserve(resource.bytes());
        entries.put(probe.snapshot(), resource);
        bytes += resource.bytes();
    }

    /** Reserve before native allocation so even an upload does not temporarily exceed the GPU budget. */
    void reserve(long size)
    {
        if (size > maximumBytes) throw new IllegalArgumentException("Mesh exceeds cache budget");
        while (!entries.isEmpty() && (bytes + size > maximumBytes || entries.size() >= maximumEntries))
        {
            var iterator = entries.values().iterator();
            T oldest = iterator.next();
            iterator.remove();
            bytes -= oldest.bytes();
            oldest.close();
        }
    }

    void clear()
    {
        for (T resource : entries.values()) resource.close();
        entries.clear();
        bytes = 0;
    }
}
