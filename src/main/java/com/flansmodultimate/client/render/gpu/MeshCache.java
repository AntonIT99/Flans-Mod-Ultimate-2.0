package com.flansmodultimate.client.render.gpu;

import java.util.LinkedHashMap;

/** Render-thread LRU. Eviction/clear own resource disposal, including replacement. */
final class MeshCache<T extends MeshCache.Resource>
{
    interface Resource { long bytes(); void close(); }

    private static final class Entry<T>
    {
        final T resource;
        long frame;

        Entry(T resource, long frame)
        {
            this.resource = resource;
            this.frame = frame;
        }
    }

    private final LinkedHashMap<GeometryKey, Entry<T>> entries = new LinkedHashMap<>(64, 0.75F, true);
    private long maximumBytes;
    private int maximumEntries;
    private long bytes;
    private long frame;
    private long workingSetEvictions;

    MeshCache(long maximumBytes, int maximumEntries)
    {
        this.maximumBytes = maximumBytes;
        this.maximumEntries = maximumEntries;
    }

    T get(GeometryKey probe)
    {
        Entry<T> entry = entries.get(probe);
        if (entry != null) entry.frame = frame;
        if (RenderDiagnostics.enabled)
        {
            if (entry == null) RenderDiagnostics.misses++;
            else RenderDiagnostics.hits++;
        }
        return entry == null ? null : entry.resource;
    }

    long bytes() { return bytes; }
    long maximumBytes() { return maximumBytes; }
    int size() { return entries.size(); }
    int maximumEntries() { return maximumEntries; }

    /** Advance the frame that marks a mesh as in use, for telling working-set evictions from stale ones. */
    void nextFrame() { frame++; }
    long frame() { return frame; }

    /** Apply a changed budget at once, evicting least recently used meshes beyond it. */
    void limits(long maximumBytes, int maximumEntries)
    {
        this.maximumBytes = maximumBytes;
        this.maximumEntries = maximumEntries;
        while (!entries.isEmpty() && (bytes > maximumBytes || entries.size() > maximumEntries)) evictOldest();
    }

    void put(GeometryKey probe, T resource)
    {
        Entry<T> previous = entries.remove(probe);
        if (previous != null) { bytes -= previous.resource.bytes(); previous.resource.close(); }
        reserve(resource.bytes());
        entries.put(probe.snapshot(), new Entry<>(resource, frame));
        bytes += resource.bytes();
        RenderDiagnostics.countResident(bytes, entries.size());
    }

    /** Reserve before native allocation so even an upload does not temporarily exceed the GPU budget. */
    void reserve(long size)
    {
        if (size > maximumBytes) throw new IllegalArgumentException("Mesh exceeds cache budget");
        while (!entries.isEmpty() && (bytes + size > maximumBytes || entries.size() >= maximumEntries)) evictOldest();
    }

    private void evictOldest()
    {
        var iterator = entries.values().iterator();
        Entry<T> oldest = iterator.next();
        iterator.remove();
        bytes -= oldest.resource.bytes();
        oldest.resource.close();
        // Drawn this frame or the last: the visible working set no longer fits and meshes are re-uploaded.
        boolean workingSet = oldest.frame >= frame - 1;
        if (workingSet) workingSetEvictions++;
        if (RenderDiagnostics.enabled)
        {
            RenderDiagnostics.evictions++;
            if (workingSet) RenderDiagnostics.workingSetEvictions++;
        }
    }

    /** Working-set evictions since the previous call, which drive the automatic budget's growth. */
    long takeWorkingSetEvictions()
    {
        long count = workingSetEvictions;
        workingSetEvictions = 0;
        return count;
    }

    void clear()
    {
        for (Entry<T> entry : entries.values()) entry.resource.close();
        entries.clear();
        bytes = 0;
    }
}
